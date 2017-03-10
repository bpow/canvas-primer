package org.renci.canvas.primer.refseq.commands;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.annotation.model.AnnotationGene;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalId;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalIdPK;
import org.renci.canvas.dao.annotation.model.AnnotationGeneSynonym;
import org.renci.canvas.dao.annotation.model.AnnotationGeneSynonymPK;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.refseq.model.CDSECNumber;
import org.renci.canvas.dao.refseq.model.CDSECNumberPK;
import org.renci.canvas.dao.refseq.model.CDSTranslationException;
import org.renci.canvas.dao.refseq.model.CDSTranslationExceptionPK;
import org.renci.canvas.dao.refseq.model.Feature;
import org.renci.canvas.dao.refseq.model.FeatureType;
import org.renci.canvas.dao.refseq.model.GroupingType;
import org.renci.canvas.dao.refseq.model.RefSeqCodingSequence;
import org.renci.canvas.dao.refseq.model.RefSeqGene;
import org.renci.canvas.dao.refseq.model.RegionGroup;
import org.renci.canvas.dao.refseq.model.RegionGroupRegion;
import org.renci.canvas.dao.refseq.model.RegionGroupRegionPK;
import org.renci.canvas.dao.refseq.model.Transcript;
import org.renci.canvas.dao.refseq.model.TranscriptMaps;
import org.renci.canvas.dao.refseq.model.TranscriptMapsExons;
import org.renci.canvas.dao.refseq.model.TranscriptMapsExonsPK;
import org.renci.canvas.dao.refseq.model.TranscriptRefSeqVersion;
import org.renci.canvas.dao.refseq.model.TranscriptRefSeqVersionPK;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.renci.gbff.GBFFFilter;
import org.renci.gbff.GBFFManager;
import org.renci.gbff.filter.GBFFAndFilter;
import org.renci.gbff.filter.GBFFFeatureSourceOrganismNameFilter;
import org.renci.gbff.filter.GBFFFeatureTypeNameFilter;
import org.renci.gbff.filter.GBFFSequenceAccessionPrefixFilter;
import org.renci.gbff.filter.GBFFSourceOrganismNameFilter;
import org.renci.gbff.model.Sequence;
import org.renci.gbff.model.TranslationException;
import org.renci.gff3.GFF3Manager;
import org.renci.gff3.filters.GFF3AttributeValueFilter;
import org.renci.gff3.model.GFF3Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "refseq", name = "persist", description = "Download & persist RefSeq data")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    private static final Pattern locationPattern = Pattern.compile("(<start>\\d+)\\.+?(<stop>\\d+)?");

    private static final GBFFManager gbffMgr = GBFFManager.getInstance(1, false);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private PrimerDAOBeanService annotationDAOBeanService;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();

            try {
                List<String> remoteFileNames = FTPFactory.ncbiListRemoteFiles("/refseq/release/release-catalog", "release");
                if (CollectionUtils.isEmpty(remoteFileNames)) {
                    logger.error("No remote files found to get version");
                    return;
                }

                List<GroupingType> allGroupingTypes = canvasDAOBeanService.getGroupingTypeDAO().findAll();

                String first = remoteFileNames.get(0);
                String refseqVersion = first.substring(7, first.indexOf("."));
                logger.info("refseqVersion = {}", refseqVersion);

                Path outputPath = Paths.get(System.getProperty("karaf.data"), "tmp", "refseq");
                File outputDir = outputPath.toFile();
                outputDir.mkdirs();

                List<File> gbffFiles = FTPFactory.ncbiDownloadFiles(outputDir, "/refseq/release/vertebrate_mammalian",
                        "vertebrate_mammalian", "rna.gbff.gz");

                Path mappingsPath = Paths.get(System.getProperty("karaf.data"), "tmp", "refseq", "mappings");
                File mappingsDir = mappingsPath.toFile();
                mappingsDir.mkdirs();

                List<File> alignmentFiles = FTPFactory.ncbiDownloadFiles(mappingsDir, "/refseq/H_sapiens/alignments", "GCF", "gff3");
                List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();
                GenomeRef genomeRef = allGenomeRefs.stream().max((a, b) -> a.getId().compareTo(b.getId())).get();
                logger.info(genomeRef.toString());

                List<File> alignmentFiles2Use = new ArrayList<>();
                if (genomeRef.getName().equals("38.2")) {
                    String pipelineVersion = "GCF_000001405.28";
                    alignmentFiles2Use.addAll(alignmentFiles.stream()
                            .filter(a -> a.getName().contains(pipelineVersion) && !a.getName().contains("refseqgene"))
                            .collect(Collectors.toList()));
                }

                // List<File> fnaFiles = FTPFactory.ncbiDownloadFiles(outputDir, "/refseq/release/vertebrate_mammalian",
                // "vertebrate_mammalian",
                // "rna.fna.gz");

                List<GBFFFilter> filters = Arrays.asList(new GBFFFilter[] {
                        new GBFFSequenceAccessionPrefixFilter(Arrays.asList(new String[] { "NM_", "NR_", "XM_", "XR_" })),
                        new GBFFSourceOrganismNameFilter("Homo sapiens"), new GBFFFeatureSourceOrganismNameFilter("Homo sapiens"),
                        new GBFFFeatureTypeNameFilter("CDS"), new GBFFFeatureTypeNameFilter("source") });

                GBFFAndFilter gbffFilter = new GBFFAndFilter(filters);

                for (File f : gbffFiles) {

                    logger.info("parsing GenBankFlatFile: {}", f.getAbsolutePath());
                    List<Sequence> sequenceList = gbffMgr.deserialize(gbffFilter, f);

                    if (CollectionUtils.isEmpty(sequenceList)) {
                        logger.warn("no sequences found");
                        continue;
                    }

                    logger.info("sequenceList.size(): {}", sequenceList.size());

                    ExecutorService es = Executors.newFixedThreadPool(6);

                    for (Sequence sequence : sequenceList) {
                        es.submit(() -> {
                            try {

                                Transcript transcript = persistTranscript(refseqVersion, sequence);

                                List<org.renci.gbff.model.Feature> features = sequence.getFeatures();

                                persistGenes(refseqVersion, transcript,
                                        allGroupingTypes.stream().filter(a -> a.getName().equals("single")).findAny().get(), features);

                                persistCodingSequence(refseqVersion, transcript,
                                        allGroupingTypes.stream().filter(a -> a.getName().equals("single")).findAny().get(), features);

                                persistFeatures(refseqVersion, transcript, allGroupingTypes, features);

                                persistMappings(refseqVersion, genomeRef, transcript, alignmentFiles2Use);

                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                        });
                    }
                    es.shutdown();
                    es.awaitTermination(1L, TimeUnit.DAYS);

                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            long end = System.currentTimeMillis();
            logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000D));

        });

        return null;
    }

    private void persistMappings(String refseqVersion, GenomeRef genomeRef, Transcript transcript, List<File> alignmentFiles)
            throws CANVASDAOException {
        logger.debug("ENTERING persistMappings(String, Transcript, List<File>)");
        List<GFF3Record> records = new ArrayList<>();

        for (File alignmentFile : alignmentFiles) {
            GFF3Manager gff3Mgr = GFF3Manager.getInstance(alignmentFile);
            List<GFF3Record> results = gff3Mgr.deserialize(new GFF3AttributeValueFilter("Target", transcript.getVersionId()));
            if (CollectionUtils.isNotEmpty(results)) {
                results.forEach(records::add);
            }
        }

        String genomeReferenceAccession = records.stream().map(a -> a.getSequenceId()).distinct().collect(Collectors.joining());
        String strand = records.stream().map(a -> a.getStrand().getSymbol()).distinct().collect(Collectors.joining());
        String identity = records.stream().map(a -> a.getAttributes().get("identity")).distinct().collect(Collectors.joining());

        TranscriptMaps transcriptMaps = new TranscriptMaps();

        transcriptMaps.setGenomeRef(genomeRef);
        transcriptMaps.setGenomeRefSeq(canvasDAOBeanService.getGenomeRefSeqDAO().findById(genomeReferenceAccession));
        transcriptMaps.setStrand(strand);
        transcriptMaps.setMapCount(canvasDAOBeanService.getTranscriptMapsDAO().findNextMapCount());
        transcriptMaps.setIdentity(Double.valueOf(identity) * 100D);
        transcriptMaps.setScore(transcriptMaps.getIdentity());
        transcriptMaps.setExonCount(records.size());
        transcriptMaps.setId(canvasDAOBeanService.getTranscriptMapsDAO().save(transcriptMaps));
        logger.info(transcriptMaps.toString());

        records.sort((a, b) -> a.getStart().compareTo(b.getStart()));

        int exonNum = 0;
        for (GFF3Record record : records) {

            TranscriptMapsExonsPK exonPK = new TranscriptMapsExonsPK(transcriptMaps.getId(), exonNum++);
            TranscriptMapsExons exon = new TranscriptMapsExons(exonPK);
            exon.setTranscriptMaps(transcriptMaps);

            String target = record.getAttributes().get("Target");
            String[] parts = target.split(" ");

            String exonStart = parts[1];
            String exonStop = parts[2];

            exon.setContigStart(record.getStart());
            exon.setContigEnd(record.getEnd());
            exon.setTranscriptStart(Integer.valueOf(exonStart));
            exon.setTranscriptEnd(Integer.valueOf(exonStop));

            canvasDAOBeanService.getTranscriptMapsExonsDAO().save(exon);
            logger.info(exon.toString());
            transcriptMaps.getExons().add(exon);

        }

        canvasDAOBeanService.getTranscriptMapsDAO().save(transcriptMaps);

    }

    private void persistFeatures(String refseqVersion, Transcript transcript, List<GroupingType> allGroupingTypes,
            List<org.renci.gbff.model.Feature> features) throws CANVASDAOException {
        logger.debug("ENTERING persistFeatures(String, Transcript, List<GroupingType>, List<Feature>)");

        List<String> featureExclusionList = Arrays.asList("CDS", "gene", "STS", "variation", "exon", "source", "precursor_RNA");
        List<org.renci.gbff.model.Feature> filteredFeatures = features.stream().filter(a -> !featureExclusionList.contains(a.getType()))
                .collect(Collectors.toList());

        for (org.renci.gbff.model.Feature gbffFeature : filteredFeatures) {
            logger.info(gbffFeature.toString());

            FeatureType featureType = canvasDAOBeanService.getFeatureTypeDAO().findById(gbffFeature.getType());
            if (featureType == null) {
                canvasDAOBeanService.getFeatureTypeDAO().save(featureType);
                featureType = canvasDAOBeanService.getFeatureTypeDAO().findById(gbffFeature.getType());
            }
            Map<String, String> qualifiers = gbffFeature.getQualifiers();
            String note = qualifiers.get("note");
            String location = gbffFeature.getLocation();

            List<Pair<String, String>> rangeList = new ArrayList<>();

            GroupingType groupingType;
            if (gbffFeature.getLocation().startsWith("join")) {
                groupingType = allGroupingTypes.stream().filter(a -> a.getName().equals("join")).findAny().get();
                Arrays.asList(location.substring(6, location.length() - 1).split(",")).forEach(a -> {
                    Matcher m = locationPattern.matcher(a);
                    if (m.find()) {
                        rangeList.add(Pair.of(m.group("start"), m.group("stop")));
                    }
                });
            } else if (gbffFeature.getLocation().startsWith("order")) {
                Arrays.asList(location.substring(7, location.length() - 1).split(",")).forEach(a -> {
                    Matcher m = locationPattern.matcher(a);
                    if (m.find()) {
                        rangeList.add(Pair.of(m.group("start"), m.group("stop")));
                    }
                });
                groupingType = allGroupingTypes.stream().filter(a -> a.getName().equals("order")).findAny().get();
            } else {
                Matcher m = locationPattern.matcher(location);
                if (m.find()) {
                    rangeList.add(Pair.of(m.group("start"), m.group("stop")));
                }
                groupingType = allGroupingTypes.stream().filter(a -> a.getName().equals("single")).findAny().get();
            }
            logger.info(groupingType.toString());

            RegionGroup regionGroup = new RegionGroup();
            regionGroup.setTranscript(transcript);
            regionGroup.setGroupingType(groupingType);
            regionGroup.setId(canvasDAOBeanService.getRegionGroupDAO().save(regionGroup));
            logger.info(regionGroup.toString());

            for (Pair<String, String> pair : rangeList) {

                String startLocation = pair.getLeft();
                String stopLocation = pair.getRight();

                String startType = "exact";
                String stopType = "exact";

                if (pair.getLeft().startsWith("<")) {
                    startType = "less-than";
                    startLocation = startLocation.substring(1, startLocation.length());
                } else if (pair.getLeft().startsWith(">")) {
                    startType = "greater-than";
                    startLocation = startLocation.substring(1, startLocation.length());
                }

                if (pair.getRight().startsWith("<")) {
                    stopType = "less-than";
                    stopLocation = stopLocation.substring(1, stopLocation.length());
                } else if (pair.getRight().startsWith(">")) {
                    stopType = "greater-than";
                    stopLocation = stopLocation.substring(1, stopLocation.length());
                }

                RegionGroupRegionPK rgrPK = new RegionGroupRegionPK(Integer.valueOf(startLocation), Integer.valueOf(stopLocation),
                        startType, stopType, regionGroup.getId());

                RegionGroupRegion rgr = new RegionGroupRegion(rgrPK);
                rgr.setRegionGroup(regionGroup);
                logger.info(rgr.toString());
                canvasDAOBeanService.getRegionGroupRegionDAO().save(rgr);

            }

            Feature feature = new Feature(refseqVersion, note);
            feature.setType(featureType);
            feature.setRegionGroup(regionGroup);
            logger.info(feature.toString());
            canvasDAOBeanService.getFeatureDAO().save(feature);

        }

    }

    private void persistCodingSequence(String refseqVersion, Transcript transcript, GroupingType singleGroupingType,
            List<org.renci.gbff.model.Feature> features) throws CANVASDAOException {
        logger.debug("ENTERING persistCodingSequence(String, Transcript, List<Feature>)");
        Optional<org.renci.gbff.model.Feature> optionalCodingSequenceFeature = features.stream().filter(a -> "CDS".equals(a.getType()))
                .findAny();
        if (optionalCodingSequenceFeature.isPresent()) {
            org.renci.gbff.model.Feature codingSequenceFeature = optionalCodingSequenceFeature.get();
            logger.info(codingSequenceFeature.toString());

            Map<String, String> qualifiers = codingSequenceFeature.getQualifiers();
            Map<String, String> dbxrefMap = codingSequenceFeature.getDbXRefs();
            List<TranslationException> translationExceptionList = codingSequenceFeature.getTranslationExceptions();

            String note = qualifiers.get("note");
            String codonStart = qualifiers.get("codon_start");
            String product = qualifiers.get("product");
            String proteinId = qualifiers.get("protein_id");
            String translation = qualifiers.get("translation");
            String description = qualifiers.get("desc");
            String ecNumber = qualifiers.get("EC_number");

            RefSeqCodingSequence refseqCodingSequence = new RefSeqCodingSequence(refseqVersion, proteinId, product,
                    Integer.valueOf(codonStart), description, translation, note);

            List<RefSeqCodingSequence> foundRefSeqCodingSequences = canvasDAOBeanService.getRefSeqCodingSequenceDAO()
                    .findByExample(refseqCodingSequence);

            if (CollectionUtils.isEmpty(foundRefSeqCodingSequences)) {
                refseqCodingSequence.setId(canvasDAOBeanService.getRefSeqCodingSequenceDAO().save(refseqCodingSequence));
            } else {
                refseqCodingSequence = foundRefSeqCodingSequences.get(0);
            }
            logger.info(refseqCodingSequence.toString());

            String location = codingSequenceFeature.getLocation();
            Integer start = null;
            Integer stop = null;

            Matcher m = locationPattern.matcher(location);
            if (m.find()) {
                String startValue = m.group("start");
                start = Integer.valueOf(startValue);
                stop = start;
                String stopValue = m.group("stop");
                if (StringUtils.isNotEmpty(stopValue)) {
                    stop = Integer.valueOf(stopValue);
                }
            }

            if (start != null && stop != null) {

                RegionGroup regionGroup = new RegionGroup();
                regionGroup.setTranscript(transcript);
                regionGroup.setGroupingType(singleGroupingType);
                regionGroup.setId(canvasDAOBeanService.getRegionGroupDAO().save(regionGroup));

                RegionGroupRegionPK rgrPK = new RegionGroupRegionPK(start, stop, "exact", "exact", regionGroup.getId());
                RegionGroupRegion rgr = new RegionGroupRegion(rgrPK);
                logger.info(rgr.toString());
                rgr.setRegionGroup(regionGroup);
                canvasDAOBeanService.getRegionGroupRegionDAO().save(rgr);

                refseqCodingSequence.getLocations().add(regionGroup);
                canvasDAOBeanService.getRefSeqCodingSequenceDAO().save(refseqCodingSequence);

            }

            if (StringUtils.isNotEmpty(ecNumber)) {
                CDSECNumberPK key = new CDSECNumberPK(refseqCodingSequence.getId(), ecNumber);
                CDSECNumber foundCDSECNumber = canvasDAOBeanService.getCDSECNumberDAO().findById(key);
                if (foundCDSECNumber == null) {
                    CDSECNumber cdsECNumber = new CDSECNumber(key);
                    cdsECNumber.setRefseqCodingSequence(refseqCodingSequence);
                    logger.info(cdsECNumber.toString());
                    canvasDAOBeanService.getCDSECNumberDAO().save(cdsECNumber);
                }
            }

            if (CollectionUtils.isNotEmpty(translationExceptionList)) {

                for (TranslationException te : translationExceptionList) {
                    CDSTranslationExceptionPK key = new CDSTranslationExceptionPK(refseqCodingSequence.getId(), te.getRange().getMinimum());
                    CDSTranslationException foundCDSTranslationException = canvasDAOBeanService.getCDSTranslationExceptionDAO()
                            .findById(key);
                    if (foundCDSTranslationException == null) {
                        CDSTranslationException cdsTranslationException = new CDSTranslationException(key);
                        cdsTranslationException.setRefseqCodingSequence(refseqCodingSequence);
                        cdsTranslationException.setStopLocation(te.getRange().getMaximum());
                        cdsTranslationException.setAminoAcid(te.getAminoAcid());
                        logger.info(cdsTranslationException.toString());
                        canvasDAOBeanService.getCDSTranslationExceptionDAO().save(cdsTranslationException);
                    }

                }

            }

        }
    }

    private void persistGenes(String refseqVersion, Transcript transcript, GroupingType singleGroupingType,
            List<org.renci.gbff.model.Feature> features) throws CANVASDAOException {
        logger.debug("ENTERING persistCodingSequence(String, Transcript, GroupingType, List<Feature>)");

        Optional<org.renci.gbff.model.Feature> optionalGeneFeature = features.stream().filter(a -> "gene".equals(a.getType())).findAny();
        if (optionalGeneFeature.isPresent()) {
            org.renci.gbff.model.Feature geneFeature = optionalGeneFeature.get();
            logger.info(geneFeature.toString());

            Map<String, String> qualifiers = geneFeature.getQualifiers();

            String geneName = qualifiers.get("gene");
            String geneDesc = qualifiers.get("note");
            String geneSynonyms = qualifiers.get("gene_synonym");

            RefSeqGene refseqGene = new RefSeqGene(refseqVersion, geneName, geneDesc);

            List<RefSeqGene> foundRefseqGenes = canvasDAOBeanService.getRefSeqGeneDAO().findByExample(refseqGene);
            if (CollectionUtils.isEmpty(foundRefseqGenes)) {
                refseqGene.setId(canvasDAOBeanService.getRefSeqGeneDAO().save(refseqGene));
            } else {
                refseqGene = foundRefseqGenes.get(0);
            }

            String location = geneFeature.getLocation();
            Integer start = null;
            Integer stop = null;

            Matcher m = locationPattern.matcher(location);
            if (m.find()) {
                String startValue = m.group("start");
                start = Integer.valueOf(startValue);
                stop = start;
                String stopValue = m.group("stop");
                if (StringUtils.isNotEmpty(stopValue)) {
                    stop = Integer.valueOf(stopValue);
                }
            }

            if (start != null && stop != null) {

                RegionGroup regionGroup = new RegionGroup();
                regionGroup.setTranscript(transcript);
                regionGroup.setGroupingType(singleGroupingType);

                Set<RegionGroup> regionGroups;
                List<RegionGroup> foundRegionGroups = canvasDAOBeanService.getRegionGroupDAO()
                        .findByTranscriptIdAndGroupingType(transcript.getVersionId(), singleGroupingType.getName());
                if (CollectionUtils.isEmpty(foundRegionGroups)) {
                    regionGroup.setId(canvasDAOBeanService.getRegionGroupDAO().save(regionGroup));
                    regionGroups = transcript.getRegionGroups();
                } else {
                    regionGroup = foundRegionGroups.get(0);
                    regionGroups = new HashSet<>(foundRegionGroups);
                }
                logger.info(regionGroup.toString());

                transcript.setRegionGroups(regionGroups);
                canvasDAOBeanService.getTranscriptDAO().save(transcript);

                RegionGroupRegionPK rgrPK = new RegionGroupRegionPK(start, stop, "exact", "exact", regionGroup.getId());

                RegionGroupRegion foundRGR = canvasDAOBeanService.getRegionGroupRegionDAO().findById(rgrPK);
                if (foundRGR == null) {
                    RegionGroupRegion rgr = new RegionGroupRegion(rgrPK);
                    rgr.setRegionGroup(regionGroup);
                    logger.info(rgr.toString());
                    canvasDAOBeanService.getRegionGroupRegionDAO().save(rgr);
                }

                refseqGene.getLocations().add(regionGroup);
                canvasDAOBeanService.getRefSeqGeneDAO().save(refseqGene);

            }

            AnnotationGene annotationGene = new AnnotationGene(geneName, geneDesc);
            List<AnnotationGene> foundAnnotationGenes = canvasDAOBeanService.getAnnotationGeneDAO().findByExample(annotationGene);
            if (CollectionUtils.isEmpty(foundAnnotationGenes)) {
                Integer geneId = canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);
                annotationGene.setId(geneId);
            } else {
                annotationGene = foundAnnotationGenes.get(0);
            }
            logger.info(annotationGene.toString());

            List<AnnotationGeneExternalId> annotationGeneExternalIds = canvasDAOBeanService.getAnnotationGeneExternalIdDAO()
                    .findByExternalIdAndNamespace(refseqGene.getId(), "refseq");
            Set<AnnotationGeneExternalId> externalIdSet = new HashSet<>(annotationGeneExternalIds);

            AnnotationGeneExternalIdPK annotationGeneExternalIdPK = new AnnotationGeneExternalIdPK(refseqGene.getId(),
                    annotationGene.getId(), "refseq", refseqVersion);

            if (CollectionUtils.isEmpty(annotationGeneExternalIds)
                    || (CollectionUtils.isNotEmpty(annotationGeneExternalIds) && !annotationGeneExternalIds.stream()
                            .filter(a -> a.getKey().equals(annotationGeneExternalIdPK)).findAny().isPresent())) {

                AnnotationGeneExternalId annotationGeneExternalId = new AnnotationGeneExternalId(annotationGeneExternalIdPK);
                annotationGeneExternalId.setGene(annotationGene);
                logger.info(annotationGeneExternalId.toString());
                canvasDAOBeanService.getAnnotationGeneExternalIdDAO().save(annotationGeneExternalId);
                externalIdSet.add(annotationGeneExternalId);

            }

            annotationGene.setExternals(externalIdSet);
            canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);

            List<AnnotationGeneSynonym> annotationGeneSynonyms = canvasDAOBeanService.getAnnotationGeneSynonymDAO()
                    .findByGeneId(annotationGene.getId());

            Set<AnnotationGeneSynonym> annotationGeneSynonymSet = new HashSet<>(annotationGeneSynonyms);

            List<String> synonymList = new ArrayList<>();
            if (StringUtils.isNotEmpty(geneSynonyms)) {
                if (geneSynonyms.contains(";")) {
                    synonymList.addAll(Arrays.asList(geneSynonyms.split(";")));
                } else {
                    synonymList.add(geneSynonyms);
                }
            }

            if (CollectionUtils.isNotEmpty(synonymList)) {
                for (String synonym : synonymList) {
                    AnnotationGeneSynonymPK annotationGeneSynonymPK = new AnnotationGeneSynonymPK(annotationGene.getId(), synonym);
                    AnnotationGeneSynonym foundAnnotationGeneSynonym = canvasDAOBeanService.getAnnotationGeneSynonymDAO()
                            .findById(annotationGeneSynonymPK);
                    if (foundAnnotationGeneSynonym == null) {
                        AnnotationGeneSynonym annotationGeneSynonym = new AnnotationGeneSynonym(annotationGeneSynonymPK);
                        annotationGeneSynonym.setGene(annotationGene);
                        annotationGeneSynonym.setKey(canvasDAOBeanService.getAnnotationGeneSynonymDAO().save(annotationGeneSynonym));
                        logger.info(annotationGeneSynonym.toString());
                        annotationGeneSynonymSet.add(annotationGeneSynonym);
                    } else {
                        logger.info(foundAnnotationGeneSynonym.toString());
                        annotationGeneSynonymSet.add(foundAnnotationGeneSynonym);
                    }
                }
            }

            annotationGene.setSynonyms(annotationGeneSynonymSet);
            canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);

        }

    }

    private Transcript persistTranscript(String refseqVersion, Sequence sequence) throws CANVASDAOException {
        logger.debug("ENTERING persistTranscript(String, Sequence)");

        String versionedAccession = sequence.getVersion().substring(0, sequence.getVersion().indexOf(" "));
        StringBuilder seq = new StringBuilder();
        sequence.getOrigin().forEach(a -> seq.append(a.getSequence()));
        Transcript transcript = new Transcript(versionedAccession, sequence.getAccession(), seq.toString());

        Transcript foundTranscript = canvasDAOBeanService.getTranscriptDAO().findById(versionedAccession);
        if (foundTranscript == null) {
            canvasDAOBeanService.getTranscriptDAO().save(transcript);
        }

        TranscriptRefSeqVersionPK transcriptRefSeqVersionPK = new TranscriptRefSeqVersionPK(versionedAccession, refseqVersion);
        TranscriptRefSeqVersion foundTranscriptRefSeqVersion = canvasDAOBeanService.getTranscriptRefSeqVersionDAO()
                .findById(transcriptRefSeqVersionPK);
        if (foundTranscriptRefSeqVersion == null) {
            TranscriptRefSeqVersion transcriptRefSeqVersion = new TranscriptRefSeqVersion(transcriptRefSeqVersionPK);
            transcriptRefSeqVersion.setTranscript(transcript);
            logger.info(transcriptRefSeqVersion.toString());
            canvasDAOBeanService.getTranscriptRefSeqVersionDAO().save(transcriptRefSeqVersion);

            transcript.getRefseqVersions().add(transcriptRefSeqVersion);
            canvasDAOBeanService.getTranscriptDAO().save(transcript);
        }

        logger.info(transcript.toString());
        return transcript;
    }

}
