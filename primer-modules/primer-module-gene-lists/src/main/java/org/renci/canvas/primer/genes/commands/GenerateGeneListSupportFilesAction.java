package org.renci.canvas.primer.genes.commands;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.annotation.model.AnnotationGene;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalId;
import org.renci.canvas.dao.annotation.model.AnnotationGeneSynonym;
import org.renci.canvas.dao.clinbin.model.DXExons;
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

@Command(scope = "gene-lists", name = "generate-gene-list-support-files", description = "")
@Service
public class GenerateGeneListSupportFilesAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(GenerateGeneListSupportFilesAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Option(name = "--diagnosticResultVersionId", description = "DiagnosticResultVersion identifier", required = false, multiValued = false)
    private Integer diagnosticResultVersionId;

    @Option(name = "--persist-exons", description = "Persist DXExons", required = false, multiValued = false)
    private Boolean persistExons = Boolean.TRUE;

    @Option(name = "--outputDirectory", description = "Output Directory", required = false, multiValued = false)
    private String outputDirectory = "/tmp";

    public GenerateGeneListSupportFilesAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().submit(() -> {

            try {

                DiagnosticResultVersion tmpDiagnosticResultVersion = null;

                if (diagnosticResultVersionId != null) {
                    tmpDiagnosticResultVersion = canvasDAOBeanService.getDiagnosticResultVersionDAO().findById(diagnosticResultVersionId);
                }

                if (tmpDiagnosticResultVersion == null) {
                    // get latest
                    List<DiagnosticResultVersion> allDiagnosticResultVersions = canvasDAOBeanService.getDiagnosticResultVersionDAO()
                            .findAll();
                    allDiagnosticResultVersions.sort((a, b) -> b.getId().compareTo(a.getId()));
                    tmpDiagnosticResultVersion = allDiagnosticResultVersions.get(0);
                }

                final DiagnosticResultVersion diagnosticResultVersion = tmpDiagnosticResultVersion;
                logger.info(diagnosticResultVersion.toString());

                List<DiagnosticGene> diagnosticGenes = canvasDAOBeanService.getDiagnosticGeneDAO()
                        .findByGroupVersionAndExternalNamespaceAndVersion(diagnosticResultVersion.getDiagnosticBinGroupVersion(), "refseq",
                                diagnosticResultVersion.getRefseqVersion().toString());

                if (CollectionUtils.isNotEmpty(diagnosticGenes)) {
                    logger.info("diagnosticGenes.size(): {}", diagnosticGenes.size());

                    Set<Interval> intervals = new HashSet<>();
                    List<String> prefixExclude = Arrays.asList("NR_", "XR_");

                    ExecutorService es = Executors.newFixedThreadPool(4);

                    for (DiagnosticGene diagnosticGene : diagnosticGenes) {

                        es.submit(() -> {

                            try {
                                logger.info(diagnosticGene.toString());

                                AnnotationGene annotationGene = diagnosticGene.getGene();

                                List<AnnotationGeneExternalId> annotationGeneExternalIds = canvasDAOBeanService
                                        .getAnnotationGeneExternalIdDAO().findByAnnotationGeneId(annotationGene.getId());

                                if (CollectionUtils.isNotEmpty(annotationGeneExternalIds)) {
                                    logger.debug("annotationGeneExternalIds.size(): {}", annotationGeneExternalIds.size());

                                    List<AnnotationGeneExternalId> filteredAnnotationGeneExternalIds = annotationGeneExternalIds.stream()
                                            .filter(b -> "refseq".equals(b.getId().getNamespace()) && diagnosticResultVersion
                                                    .getRefseqVersion().toString().equals(b.getId().getNamespaceVer()))
                                            .collect(Collectors.toList());

                                    if (CollectionUtils.isNotEmpty(filteredAnnotationGeneExternalIds)) {
                                        logger.debug("filteredExternals.size(): {}", filteredAnnotationGeneExternalIds.size());

                                        for (AnnotationGeneExternalId externalAnnotationGene : filteredAnnotationGeneExternalIds) {

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
                                                            diagnosticResultVersion.getGenomeRef().getId());

                                            List<TranscriptMaps> filteredTranscriptMapsList = transcriptMapsList.stream()
                                                    .filter(a -> a.getGenomeRefSeq().getSequenceType().getId().equals("Chromosome")
                                                            && !prefixExclude.contains(a.getTranscript().getId().substring(0, 3)))
                                                    .collect(Collectors.toList());

                                            Map<String, List<TranscriptMaps>> transcript2TranscriptMapsMap = new HashMap<>();

                                            for (TranscriptMaps transcriptMaps : filteredTranscriptMapsList) {
                                                String key = transcriptMaps.getTranscript().getId();
                                                if (!transcript2TranscriptMapsMap.containsKey(key)) {
                                                    transcript2TranscriptMapsMap.put(key, new ArrayList<>());
                                                }
                                                transcript2TranscriptMapsMap.get(key).add(transcriptMaps);
                                            }

                                            for (TranscriptMaps transcriptMaps : filteredTranscriptMapsList) {

                                                List<RefSeqCodingSequence> refSeqCodingSequenceList = canvasDAOBeanService
                                                        .getRefSeqCodingSequenceDAO().findByRefSeqVersionAndTranscriptId(
                                                                diagnosticResultVersion.getRefseqVersion().toString(),
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
                                                                contigStart = transcriptMapsExonsContigRange.getMinimum()
                                                                        + (proteinRange.getMinimum()
                                                                                - transcriptMapsExonsTranscriptRange.getMinimum());
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

                                                    if (persistExons) {

                                                        Integer mapNum = transcript2TranscriptMapsMap
                                                                .get(transcriptMaps.getTranscript().getId()).indexOf(transcriptMaps) + 1;

                                                        List<DXExons> foundExons = canvasDAOBeanService.getDXExonsDAO()
                                                                .findByListVersionAndTranscriptAndExonAndMapNum(
                                                                        diagnosticResultVersion.getId(),
                                                                        transcriptMaps.getTranscript().getId(), idx, mapNum);

                                                        if (CollectionUtils.isEmpty(foundExons)) {
                                                            DXExons dxExons = new DXExons(diagnosticResultVersion.getId(), annotationGene,
                                                                    transcriptMaps.getTranscript().getId(), idx,
                                                                    transcriptMaps.getGenomeRefSeq().getId(), contigRange.getMinimum() - 2,
                                                                    contigRange.getMaximum() + 2, mapNum);
                                                            dxExons.setId(canvasDAOBeanService.getDXExonsDAO().save(dxExons));
                                                            logger.info(dxExons.toString());
                                                        }

                                                    }

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

                    es.shutdown();
                    if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                        es.shutdownNow();
                    }

                    File outputDir = new File(String.format("%s/%s", outputDirectory, diagnosticResultVersion.getId()));
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                    }

                    writeDXGeneFile(intervals, outputDir);

                    writeExonsFile(intervals, outputDir);

                    writeExonsBedFile(intervals, diagnosticResultVersion.getId(), outputDir);

                    writeDXGeneBedFiles(intervals, diagnosticResultVersion.getId(), outputDir);

                }

            } catch (CANVASDAOException | InterruptedException | IOException e) {
                logger.error(e.getMessage(), e);
            }

        });
        logger.info("DONE");

        return null;
    }

    private void writeDXGeneBedFiles(Set<Interval> intervals, Integer listVersion, File outputDir) throws IOException {

        Set<Integer> dxIdSet = new HashSet<>();
        for (Interval interval : intervals) {
            dxIdSet.add(interval.getDxId());
        }

        for (Integer dxId : dxIdSet) {

            File genesByDXIdIntervalListFile = new File(outputDir, String.format("genes_dxid_%s_v_%s.bed", dxId, listVersion));

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

    private void writeExonsBedFile(Set<Interval> intervals, Integer listVersion, File outputDir) throws IOException {

        Set<String> intervalRegionSet = new HashSet<>();
        for (Interval interval : intervals) {
            intervalRegionSet.add(interval.toBedFormat());
        }

        File intervalListFile = new File(outputDir, String.format("exons_pm_0_v%s.bed", listVersion));
        try (FileWriter fw = new FileWriter(intervalListFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (String interval : intervalRegionSet) {
                bw.write(interval);
                bw.newLine();
                bw.flush();
            }
        }
    }

    private void writeExonsFile(Set<Interval> intervals, File outputDir) throws IOException {

        TreeSet<Interval> tsIntervals = new TreeSet<>();
        tsIntervals.addAll(intervals);

        File exonsFile = new File(outputDir, "exons.txt");
        try (FileWriter fw = new FileWriter(exonsFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (Interval interval : tsIntervals) {
                bw.write(interval.toStringRaw());
                bw.newLine();
                bw.flush();
            }
        }
    }

    private void writeDXGeneFile(Set<Interval> intervals, File outputDir) throws IOException {

        Set<String> geneDXPairSet = new HashSet<>();
        for (Interval interval : intervals) {
            geneDXPairSet.add(String.format("%s\t%s", interval.getGeneName(), interval.getDxId()));
        }

        File dxGenesFile = new File(outputDir, "dx_genes.txt");
        try (FileWriter fw = new FileWriter(dxGenesFile); BufferedWriter bw = new BufferedWriter(fw)) {
            for (String pair : geneDXPairSet) {
                bw.write(pair);
                bw.newLine();
                bw.flush();
            }
        }
    }

    public Integer getDiagnosticResultVersionId() {
        return diagnosticResultVersionId;
    }

    public void setDiagnosticResultVersionId(Integer diagnosticResultVersionId) {
        this.diagnosticResultVersionId = diagnosticResultVersionId;
    }

    public Boolean getPersistExons() {
        return persistExons;
    }

    public void setPersistExons(Boolean persistExons) {
        this.persistExons = persistExons;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

}
