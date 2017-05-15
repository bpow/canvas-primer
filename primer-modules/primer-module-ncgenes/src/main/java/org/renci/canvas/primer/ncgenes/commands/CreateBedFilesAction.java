package org.renci.canvas.primer.ncgenes.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.annotation.model.AnnotationGene;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalId;
import org.renci.canvas.dao.annotation.model.AnnotationGeneSynonym;
import org.renci.canvas.dao.clinbin.model.DiagnosticGene;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.renci.canvas.dao.refseq.model.RefSeqCodingSequence;
import org.renci.canvas.dao.refseq.model.RefSeqGene;
import org.renci.canvas.dao.refseq.model.RegionGroupRegion;
import org.renci.canvas.dao.refseq.model.TranscriptMaps;
import org.renci.canvas.dao.refseq.model.TranscriptMapsExons;
import org.renci.canvas.primer.commons.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ncgenes", name = "create-bed-files", description = "")
@Service
public class CreateBedFilesAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(CreateBedFilesAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    public CreateBedFilesAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        List<DiagnosticResultVersion> allDiagnosticResultVersions = canvasDAOBeanService.getDiagnosticResultVersionDAO().findAll();
        if (CollectionUtils.isNotEmpty(allDiagnosticResultVersions)) {

            allDiagnosticResultVersions.sort((a, b) -> b.getId().compareTo(a.getId()));

            DiagnosticResultVersion latestDiagnosticResultVersion = allDiagnosticResultVersions.get(0);

            List<DiagnosticGene> diagnosticGenes = canvasDAOBeanService.getDiagnosticGeneDAO()
                    .findByGroupVersionAndExternalNamespaceAndVersion(latestDiagnosticResultVersion.getDbinGroupVersion(), "refseq",
                            latestDiagnosticResultVersion.getRefseqVersion().toString());

            if (CollectionUtils.isNotEmpty(diagnosticGenes)) {

                TreeSet<Interval> intervals = new TreeSet<>();
                List<String> prefixExclude = Arrays.asList("NR_", "XR_");

                ExecutorService es = Executors.newFixedThreadPool(4);

                for (DiagnosticGene diagnosticGene : diagnosticGenes) {

                    es.submit(() -> {

                        try {
                            AnnotationGene annotationGene = diagnosticGene.getGene();

                            List<AnnotationGeneExternalId> externals = canvasDAOBeanService.getAnnotationGeneExternalIdDAO()
                                    .findByAnnotationGeneId(annotationGene.getId());

                            if (CollectionUtils.isNotEmpty(externals)) {

                                List<AnnotationGeneExternalId> filteredExternals = externals.stream()
                                        .filter(b -> "refseq".equals(b.getId().getNamespace()) && latestDiagnosticResultVersion
                                                .getRefseqVersion().toString().equals(b.getId().getNamespaceVer()))
                                        .collect(Collectors.toList());

                                if (CollectionUtils.isNotEmpty(filteredExternals)) {

                                    for (AnnotationGeneExternalId externalAnnotationGene : filteredExternals) {

                                        RefSeqGene refseqGene = canvasDAOBeanService.getRefSeqGeneDAO()
                                                .findById(externalAnnotationGene.getId().getExternalId());

                                        Optional<AnnotationGeneSynonym> optionalAnnotationGeneSynonym = annotationGene.getSynonyms()
                                                .parallelStream().filter(b -> b.getId().getSynonym().equals(refseqGene.getName()))
                                                .findAny();

                                        String geneName = null;
                                        if (optionalAnnotationGeneSynonym.isPresent()
                                                && !refseqGene.getName().equals(diagnosticGene.getGene().getPreferredName())) {
                                            geneName = diagnosticGene.getGene().getPreferredName();
                                        } else {
                                            geneName = refseqGene.getName();
                                        }

                                        List<TranscriptMaps> transcriptMapsList = canvasDAOBeanService.getTranscriptMapsDAO()
                                                .findByGeneIdAndGenomeRefId(refseqGene.getId(),
                                                        latestDiagnosticResultVersion.getGenomeRef().getId());

                                        List<TranscriptMaps> filteredTranscriptMapsList = transcriptMapsList.stream()
                                                .filter(a -> a.getGenomeRefSeq().getSequenceType().getId().equals("Chromosome")
                                                        && !prefixExclude.contains(a.getTranscript().getId().substring(0, 3)))
                                                .collect(Collectors.toList());

                                        for (TranscriptMaps transcriptMaps : filteredTranscriptMapsList) {

                                            List<RefSeqCodingSequence> refSeqCodingSequenceList = canvasDAOBeanService
                                                    .getRefSeqCodingSequenceDAO().findByRefSeqVersionAndTranscriptId(
                                                            latestDiagnosticResultVersion.getRefseqVersion().toString(),
                                                            transcriptMaps.getTranscript().getId());

                                            Range<Integer> proteinRange = null;
                                            if (CollectionUtils.isNotEmpty(refSeqCodingSequenceList)) {
                                                RefSeqCodingSequence refSeqCDS = refSeqCodingSequenceList.get(0);
                                                List<RegionGroupRegion> rgrList = canvasDAOBeanService.getRegionGroupRegionDAO()
                                                        .findByRefSeqCodingSequenceId(refSeqCDS.getId());
                                                if (CollectionUtils.isNotEmpty(rgrList)) {
                                                    proteinRange = rgrList.get(0).getId().getRegionRange();
                                                }
                                            }

                                            if ("-".equals(transcriptMaps.getStrand())) {
                                                transcriptMaps.getExons()
                                                        .sort((a, b) -> b.getId().getExonNum().compareTo(a.getId().getExonNum()));
                                            } else {
                                                transcriptMaps.getExons()
                                                        .sort((a, b) -> a.getId().getExonNum().compareTo(b.getId().getExonNum()));
                                            }

                                            int idx = 0;

                                            for (TranscriptMapsExons exons : transcriptMaps.getExons()) {

                                                Range<Integer> transcriptMapsExonsContigRange = exons.getContigRange();
                                                Range<Integer> transcriptMapsExonsTranscriptRange = exons.getTranscriptRange();

                                                Integer contigStart = transcriptMapsExonsContigRange.getMinimum();
                                                Integer contigEnd = transcriptMapsExonsContigRange.getMaximum();

                                                if (proteinRange != null) {

                                                    if (proteinRange.isAfter(transcriptMapsExonsTranscriptRange.getMaximum())) {
                                                        continue;
                                                    }

                                                    if (proteinRange.isBefore(transcriptMapsExonsTranscriptRange.getMinimum())) {
                                                        continue;
                                                    }

                                                    if ("-".equals(transcriptMaps.getStrand())) {

                                                        if (transcriptMapsExonsTranscriptRange.contains(proteinRange.getMinimum())) {
                                                            contigEnd = transcriptMapsExonsContigRange.getMaximum()
                                                                    - (proteinRange.getMinimum()
                                                                            - transcriptMapsExonsTranscriptRange.getMinimum());
                                                        }

                                                        if (transcriptMapsExonsTranscriptRange.contains(proteinRange.getMaximum())) {
                                                            contigStart = transcriptMapsExonsContigRange.getMaximum()
                                                                    - (proteinRange.getMaximum()
                                                                            - transcriptMapsExonsTranscriptRange.getMinimum());
                                                        }

                                                    }

                                                    if ("+".equals(transcriptMaps.getStrand())) {

                                                        if (transcriptMapsExonsTranscriptRange.contains(proteinRange.getMinimum())) {
                                                            contigStart = transcriptMapsExonsContigRange.getMaximum()
                                                                    - (transcriptMapsExonsTranscriptRange.getMaximum()
                                                                            - proteinRange.getMinimum());
                                                        }

                                                        if (transcriptMapsExonsTranscriptRange.contains(proteinRange.getMaximum())) {
                                                            contigEnd = transcriptMapsExonsContigRange.getMaximum()
                                                                    - (transcriptMapsExonsTranscriptRange.getMaximum()
                                                                            - proteinRange.getMaximum());
                                                        }

                                                    }
                                                }

                                                Range<Integer> contigRange = Range.between(contigStart, contigEnd);
                                                intervals.add(new Interval(transcriptMaps.getGenomeRefSeq().getId(),
                                                        contigRange.getMinimum() - 2, contigRange.getMaximum() + 2, geneName, idx++,
                                                        transcriptMaps.getTranscript().getId(), diagnosticGene.getDx().getId()));

                                            }
                                        }
                                    }
                                }
                            }
                        } catch (CANVASDAOException e) {
                            logger.error(e.getMessage(), e);
                        }

                    });

                }

                writeDXGeneFile(intervals);

                writeExonsFile(intervals);

                writeExonsBedFile(intervals, latestDiagnosticResultVersion.getId());

                writeDXGeneBedFiles(intervals, latestDiagnosticResultVersion.getId());

            }

        }

        return null;
    }

    private void writeDXGeneBedFiles(TreeSet<Interval> intervals, Integer listVersion) throws IOException {

        Set<Integer> dxIdSet = new HashSet<>();
        for (Interval interval : intervals) {
            dxIdSet.add(interval.getDxId());
        }

        for (Integer dxId : dxIdSet) {

            File genesByDXIdIntervalListFile = new File("/tmp", String.format("genes_dxid_%s_v_%s.bed", dxId, listVersion));
            try (FileWriter fw = new FileWriter(genesByDXIdIntervalListFile); BufferedWriter bw = new BufferedWriter(fw)) {

                List<Interval> dxFilteredIntervals = intervals.stream().filter(a -> a.getDxId().equals(dxId)).collect(Collectors.toList());

                Map<Pair<String, String>, List<Interval>> map = new HashMap<>();

                for (Interval interval : dxFilteredIntervals) {
                    Pair<String, String> key = Pair.of(interval.getAccession(), interval.getTranscript());
                    if (!map.containsKey(key)) {
                        map.put(key, new ArrayList<>());
                    }
                    map.get(key).add(interval);
                }

                Set<String> dxGeneIntervalValue = new HashSet<>();

                for (Pair<String, String> key : map.keySet()) {

                    List<Interval> groupedIntervals = map.get(key);

                    Interval max = null;
                    Optional<Interval> optionalInterval = groupedIntervals.stream().max((a, b) -> a.getStop().compareTo(b.getStop()));
                    if (optionalInterval.isPresent()) {
                        max = optionalInterval.get();
                    }

                    Interval min = null;
                    optionalInterval = groupedIntervals.stream().min((a, b) -> a.getStart().compareTo(b.getStart()));
                    if (optionalInterval.isPresent()) {
                        min = optionalInterval.get();
                    }

                    Range<Integer> range = Range.between(min.getStart(), max.getStop());
                    dxGeneIntervalValue.add(String.format("%s\t%s\t%s", key.getLeft(), range.getMinimum(), range.getMaximum()));

                }

                for (String i : dxGeneIntervalValue) {
                    bw.write(i);
                    bw.newLine();
                    bw.flush();
                }

            }

        }

    }

    private void writeExonsBedFile(TreeSet<Interval> intervals, Integer listVersion) throws IOException {

        Set<String> intervalRegionSet = new HashSet<>();
        for (Interval interval : intervals) {
            intervalRegionSet.add(interval.toBedFormat());
        }

        File intervalListFile = new File("/tmp", String.format("exons_pm_0_v%s.bed", listVersion));
        try (FileWriter fw = new FileWriter(intervalListFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (String interval : intervalRegionSet) {
                bw.write(interval);
                bw.newLine();
                bw.flush();
            }
        }
    }

    private void writeExonsFile(TreeSet<Interval> intervals) throws IOException {
        File exonsFile = new File("/tmp", "exons.txt");
        try (FileWriter fw = new FileWriter(exonsFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (Interval interval : intervals) {
                bw.write(interval.toStringRaw());
                bw.newLine();
                bw.flush();
            }
        }
    }

    private void writeDXGeneFile(TreeSet<Interval> intervals) throws IOException {

        Set<String> geneDXPairSet = new HashSet<>();
        for (Interval interval : intervals) {
            geneDXPairSet.add(String.format("%s\t%s", interval.getGeneName(), interval.getDxId()));
        }

        File dxGenesFile = new File("/tmp", "dx_genes.txt");
        try (FileWriter fw = new FileWriter(dxGenesFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (String pair : geneDXPairSet) {
                bw.write(pair);
                bw.newLine();
                bw.flush();
            }
        }
    }

}
