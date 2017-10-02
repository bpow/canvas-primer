package org.renci.canvas.primer.clinvar.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinvar.model.AssertionRanking;
import org.renci.canvas.dao.clinvar.model.ClinVarVersion;
import org.renci.canvas.dao.clinvar.model.ReferenceClinicalAssertion;
import org.renci.canvas.dao.clinvar.model.SubmissionClinicalAssertion;
import org.renci.canvas.dao.clinvar.model.Trait;
import org.renci.canvas.dao.clinvar.model.TraitSet;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.CanonicalAllele;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.canvas.primer.commons.UpdateDiagnosticResultVersionCallable;
import org.renci.clinvar.ClinicalSignificanceType;
import org.renci.clinvar.CommentType;
import org.renci.clinvar.MeasureSetType;
import org.renci.clinvar.MeasureTraitType;
import org.renci.clinvar.MeasureType;
import org.renci.clinvar.MeasureType.AttributeSet;
import org.renci.clinvar.PublicSetType;
import org.renci.clinvar.ReferenceAssertionType;
import org.renci.clinvar.ReferenceAssertionType.ClinVarAccession;
import org.renci.clinvar.ReleaseType;
import org.renci.clinvar.SequenceLocationType;
import org.renci.clinvar.SetElementSetType;
import org.renci.clinvar.TraitSetType;
import org.renci.clinvar.TraitType;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_38_7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistUsingSequenceLocation implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(PersistUsingSequenceLocation.class);

    private CANVASDAOBeanService canvasDAOBeanService;

    public PersistUsingSequenceLocation(CANVASDAOBeanService canvasDAOBeanService) {
        super();
        this.canvasDAOBeanService = canvasDAOBeanService;
    }

    @Override
    public Void call() throws Exception {
        logger.debug("ENTERING execute()");

        long start = System.currentTimeMillis();

        try {

            GeReSe4jBuild gerese4jBuild37 = GeReSe4jBuild_37_3.getInstance();
            gerese4jBuild37.init();

            GeReSe4jBuild gerese4jBuild38 = GeReSe4jBuild_38_7.getInstance();
            gerese4jBuild38.init();

            Path clinvarPath = Paths.get(System.getProperty("karaf.data"), "ClinVar");
            File clinvarDir = clinvarPath.toFile();
            File clinvarDirTmp = new File(clinvarDir, "tmp");
            if (!clinvarDirTmp.exists()) {
                clinvarDirTmp.mkdirs();
            }

            File clinvarXmlFile = FTPFactory.ncbiDownload(clinvarDir, "/pub/clinvar/xml", "ClinVarFullRelease_00-latest.xml.gz");

            if (clinvarXmlFile == null) {
                logger.error("Problem downloading clinvar");
                return null;
            }

            ClinVarVersion clinvarVersion = new ClinVarVersion(clinvarXmlFile.getName());
            clinvarVersion.setId(canvasDAOBeanService.getClinVarVersionDAO().save(clinvarVersion));

            List<VariantType> allVariantTypes = canvasDAOBeanService.getVariantTypeDAO().findAll();

            List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();

            // vardb_berg_38_migration has different name than GeReSe4jBuild.getVersion(), using hardcoded id instead

            // GenomeRef genomeRef37 = allGenomeRefs.stream().filter(a -> a.getName().equals(gerese4jBuild37.getBuild().getVersion()))
            // .findFirst().get();
            GenomeRef genomeRef37 = allGenomeRefs.stream().filter(a -> a.getId().equals(2)).findFirst().get();
            List<GenomeRefSeq> all37GenomeRefSeqs = canvasDAOBeanService.getGenomeRefSeqDAO().findByGenomeRefId(genomeRef37.getId());

            // GenomeRef genomeRef38 = allGenomeRefs.stream().filter(a -> a.getName().equals(gerese4jBuild38.getBuild().getVersion()))
            // .findFirst().get();
            GenomeRef genomeRef38 = allGenomeRefs.stream().filter(a -> a.getId().equals(4)).findFirst().get();
            List<GenomeRefSeq> all38GenomeRefSeqs = canvasDAOBeanService.getGenomeRefSeqDAO().findByGenomeRefId(genomeRef38.getId());

            List<PublicSetType> pstList = new ArrayList<>();

            List<File> serializedFileList = new ArrayList<>();

            logger.info("parsing: {}", clinvarXmlFile.getName());

            List<String> measureTypeExcludes = Arrays.asList("Indel", "Microsatellite", "Inversion", "Variation");

            try (FileInputStream fis = new FileInputStream(clinvarXmlFile);
                    GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue())) {

                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                XMLEventReader reader = xmlInputFactory.createXMLEventReader(gzis);

                JAXBContext jc = JAXBContext.newInstance(ReleaseType.class, ReferenceAssertionType.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();

                QName clinvarSetQName = new QName("ClinVarSet");

                XMLEvent xmlEvent = null;
                while ((xmlEvent = reader.peek()) != null) {

                    if (xmlEvent.isStartElement() && ((StartElement) xmlEvent).getName().equals(clinvarSetQName)) {

                        PublicSetType pst = unmarshaller.unmarshal(reader, PublicSetType.class).getValue();
                        ReferenceAssertionType rat = pst.getReferenceClinVarAssertion();

                        MeasureSetType measureSetType = rat.getMeasureSet();

                        if (measureSetType != null && "Variant".equals(measureSetType.getType())) {

                            List<MeasureType> measures = measureSetType.getMeasure();

                            if (CollectionUtils.isEmpty(measures)) {
                                continue;
                            }

                            asdf: for (MeasureType measureType : measures) {

                                List<AttributeSet> filters = measureType.getAttributeSet().stream()
                                        .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level"))
                                        .collect(Collectors.toList());

                                if (CollectionUtils.isEmpty(filters)) {
                                    continue;
                                }

                                if (CollectionUtils.isNotEmpty(filters) && CollectionUtils.isNotEmpty(filters.stream()
                                        .filter(a -> a.getAttribute().getValue().contains("?")).collect(Collectors.toList()))) {
                                    continue;
                                }

                                if (measureTypeExcludes.contains(measureType.getType())) {
                                    continue;
                                }

                                List<SequenceLocationType> sequenceLocationTypeList = measureType.getSequenceLocation();

                                for (SequenceLocationType sequenceLocationType : sequenceLocationTypeList) {

                                    if (sequenceLocationType.getStart() == null || sequenceLocationType.getStop() == null) {
                                        continue asdf;
                                    }

                                    if ((sequenceLocationType.getStop().intValue() - sequenceLocationType.getStart().intValue()) > 100) {
                                        continue asdf;
                                    }

                                    if (sequenceLocationType.getVariantLength() != null
                                            && sequenceLocationType.getVariantLength().intValue() > 100) {
                                        continue asdf;
                                    }

                                }

                                pstList.add(pst);
                                if ((pstList.size() % 4000) == 0) {
                                    File f = new File(clinvarDirTmp, UUID.randomUUID().toString());
                                    serializedFileList.add(f);
                                    try (FileOutputStream fos = new FileOutputStream(f);
                                            GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                                            ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                                        oos.writeObject(pstList);
                                    }
                                    pstList.clear();
                                }

                            }

                        }

                    } else {
                        reader.next();
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            File f = new File(clinvarDirTmp, UUID.randomUUID().toString());
            serializedFileList.add(f);
            try (FileOutputStream fos = new FileOutputStream(f);
                    GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                    ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                oos.writeObject(pstList);
            }
            pstList.clear();

            clinvarXmlFile.delete();

            logger.info("serializedFileList.size(): {}", serializedFileList.size());

            Set<String> clinicalSignificanceDescSet = new HashSet<>();
            Set<TraitSet> traitSets = new HashSet<>();
            Set<Trait> traits = new HashSet<>();

            logger.info("loading Trait/TraitSet/Assertions into memory");
            ExecutorService es = Executors.newFixedThreadPool(2);
            for (File serializedFile : serializedFileList) {

                es.submit(() -> {

                    List<PublicSetType> tmpPSTList = null;
                    try (FileInputStream fis = new FileInputStream(serializedFile);
                            GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue());
                            ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                        tmpPSTList = (List<PublicSetType>) ois.readObject();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }

                    List<String> clinicalSignificanceDescList = tmpPSTList.stream()
                            .map(a -> a.getReferenceClinVarAssertion().getClinicalSignificance().getDescription()).distinct()
                            .collect(Collectors.toList());
                    clinicalSignificanceDescSet.addAll(clinicalSignificanceDescList);

                    tmpPSTList.stream().map(a -> a.getReferenceClinVarAssertion()).forEach(a -> {
                        traitSets.add(new TraitSet(a.getTraitSet().getID().intValue(), a.getTraitSet().getType()));
                    });

                    tmpPSTList.stream().map(a -> a.getReferenceClinVarAssertion()).forEach(a -> {
                        List<TraitType> traitTypeList = a.getTraitSet().getTrait();
                        for (TraitType traitType : traitTypeList) {
                            Optional<SetElementSetType> preferredNameOptional = traitType.getName().stream()
                                    .filter(b -> b.getElementValue().getType().equals("Preferred")).findAny();
                            if (preferredNameOptional.isPresent()) {
                                traits.add(new Trait(traitType.getID().intValue(), traitType.getType(),
                                        preferredNameOptional.get().getElementValue().getValue()));
                            }
                        }

                    });

                });
            }
            es.shutdown();
            if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                es.shutdownNow();
            }

            logger.info("persisting AssertionRankings");
            for (String clinicalSignificanceDesc : clinicalSignificanceDescSet) {
                AssertionRanking assertionRanking = canvasDAOBeanService.getAssertionRankingDAO().findById(clinicalSignificanceDesc);
                if (assertionRanking == null) {
                    assertionRanking = new AssertionRanking(clinicalSignificanceDesc);
                    canvasDAOBeanService.getAssertionRankingDAO().save(assertionRanking);
                }
            }
            clinicalSignificanceDescSet.clear();

            logger.info("persisting TraitSets");
            es = Executors.newFixedThreadPool(3);
            for (TraitSet traitSet : traitSets) {
                es.submit(() -> {
                    try {
                        TraitSet foundTraitSet = canvasDAOBeanService.getTraitSetDAO().findById(traitSet.getId());
                        if (foundTraitSet == null) {
                            canvasDAOBeanService.getTraitSetDAO().save(traitSet);
                            logger.info(traitSet.toString());
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
            es.shutdown();
            if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                es.shutdownNow();
            }
            traitSets.clear();

            logger.info("persisting Traits");
            es = Executors.newFixedThreadPool(3);
            for (Trait trait : traits) {
                es.submit(() -> {
                    try {
                        Trait foundTrait = canvasDAOBeanService.getTraitDAO().findById(trait.getId());
                        if (foundTrait == null) {
                            canvasDAOBeanService.getTraitDAO().save(trait);
                            logger.info(trait.toString());
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
            es.shutdown();
            if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                es.shutdownNow();
            }
            traits.clear();

            logger.info("mapping TraitSets/Traits");
            es = Executors.newFixedThreadPool(2);
            for (File serializedFile : serializedFileList) {

                es.submit(() -> {

                    List<PublicSetType> tmpPSTList = null;
                    try (FileInputStream fis = new FileInputStream(serializedFile);
                            GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue());
                            ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                        tmpPSTList = (List<PublicSetType>) ois.readObject();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }

                    for (PublicSetType pst : tmpPSTList) {
                        try {
                            List<TraitType> traitTypeList = pst.getReferenceClinVarAssertion().getTraitSet().getTrait();
                            for (TraitType traitType : traitTypeList) {
                                Optional<SetElementSetType> preferredNameOptional = traitType.getName().stream()
                                        .filter(b -> b.getElementValue().getType().equals("Preferred")).findAny();
                                if (preferredNameOptional.isPresent()) {
                                    Trait trait = canvasDAOBeanService.getTraitDAO().findById(traitType.getID().intValue());
                                    TraitSet traitSet = canvasDAOBeanService.getTraitSetDAO()
                                            .findById(pst.getReferenceClinVarAssertion().getTraitSet().getID().intValue());
                                    if (CollectionUtils.isNotEmpty(traitSet.getTraits()) && !traitSet.getTraits().stream()
                                            .filter(a -> a.getId().equals(trait.getId())).findAny().isPresent()) {
                                        traitSet.getTraits().add(trait);
                                        canvasDAOBeanService.getTraitSetDAO().save(traitSet);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                });

            }
            es.shutdown();
            if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                es.shutdownNow();
            }

            List<AssertionRanking> allAssertionRankings = canvasDAOBeanService.getAssertionRankingDAO().findAll();
            List<Pair<LocatedVariant, LocatedVariant>> canonicalLocatedVariants = new ArrayList<>();

            logger.info("persisting ReferenceClinicalAssertions/LocatedVariants");
            es = Executors.newFixedThreadPool(2);
            for (File serializedFile : serializedFileList) {

                es.submit(() -> {

                    List<PublicSetType> publicSetTypeList = null;
                    try (FileInputStream fis = new FileInputStream(serializedFile);
                            GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue());
                            ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                        publicSetTypeList = (List<PublicSetType>) ois.readObject();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }

                    for (PublicSetType pst : publicSetTypeList) {

                        try {

                            List<MeasureTraitType> measureTraitTypeList = pst.getClinVarAssertion();

                            List<SubmissionClinicalAssertion> scaList = new ArrayList<>();
                            for (MeasureTraitType mtt : measureTraitTypeList) {
                                SubmissionClinicalAssertion sca = new SubmissionClinicalAssertion();
                                sca.setAccession(mtt.getClinVarAccession().getAcc());
                                if (CollectionUtils.isNotEmpty(mtt.getClinicalSignificance().getDescription())) {
                                    sca.setAssertion(mtt.getClinicalSignificance().getDescription().get(0));
                                }
                                sca.setReviewStatus(mtt.getClinicalSignificance().getReviewStatus().value());
                                sca.setUpdated(mtt.getClinVarAccession().getDateUpdated().toGregorianCalendar().getTime());
                                sca.setVersion(mtt.getClinVarAccession().getVersion().intValue());
                                sca.setId(canvasDAOBeanService.getSubmissionClinicalAssertionDAO().save(sca));
                                scaList.add(sca);
                            }

                            MeasureSetType measureSetType = pst.getReferenceClinVarAssertion().getMeasureSet();

                            ClinicalSignificanceType clinicalSignificanceType = pst.getReferenceClinVarAssertion()
                                    .getClinicalSignificance();
                            CommentType explanationType = clinicalSignificanceType.getExplanation();

                            ClinVarAccession clinvarAccession = pst.getReferenceClinVarAssertion().getClinVarAccession();

                            AssertionRanking assertionRanking = allAssertionRankings.stream()
                                    .filter(a -> a.getId().equals(clinicalSignificanceType.getDescription())).findFirst().get();

                            TraitSetType traitSetType = pst.getReferenceClinVarAssertion().getTraitSet();
                            TraitSet traitSet = canvasDAOBeanService.getTraitSetDAO().findById(traitSetType.getID().intValue());

                            if (measureSetType != null && "Variant".equals(measureSetType.getType())) {

                                List<MeasureType> measureTypes = measureSetType.getMeasure();

                                asdf: for (MeasureType measureType : measureTypes) {

                                    List<AttributeSet> filters = measureType.getAttributeSet().stream()
                                            .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level"))
                                            .collect(Collectors.toList());

                                    if (CollectionUtils.isEmpty(filters)) {
                                        continue;
                                    }

                                    if (CollectionUtils.isNotEmpty(filters) && CollectionUtils.isNotEmpty(filters.stream()
                                            .filter(a -> a.getAttribute().getValue().contains("?")).collect(Collectors.toList()))) {
                                        continue;
                                    }

                                    if (measureTypeExcludes.contains(measureType.getType())) {
                                        continue;
                                    }

                                    String measure = measureType.getType();
                                    logger.debug("measure: {}", measure);

                                    List<SequenceLocationType> sequenceLocationTypeList = measureType.getSequenceLocation();

                                    LocatedVariant locatedVariant38 = null;
                                    LocatedVariant locatedVariant37 = null;

                                    for (SequenceLocationType sequenceLocationType : sequenceLocationTypeList) {

                                        if (sequenceLocationType.getStart() == null || sequenceLocationType.getStop() == null) {
                                            continue asdf;
                                        }

                                        if ((sequenceLocationType.getStop().intValue()
                                                - sequenceLocationType.getStart().intValue()) > 100) {
                                            continue asdf;
                                        }

                                        if (sequenceLocationType.getVariantLength() != null
                                                && sequenceLocationType.getVariantLength().intValue() > 100) {
                                            continue asdf;
                                        }

                                        String accession = sequenceLocationType.getAccession();

                                        ReferenceClinicalAssertion rca = new ReferenceClinicalAssertion();
                                        if (explanationType != null && StringUtils.isNotEmpty(explanationType.getValue())) {
                                            rca.setExplanation(explanationType.getValue());
                                        }
                                        rca.setAccession(clinvarAccession.getAcc());
                                        rca.setVersion(clinvarAccession.getVersion().intValue());
                                        rca.setCreated(pst.getReferenceClinVarAssertion().getDateCreated().toGregorianCalendar().getTime());
                                        rca.setUpdated(clinvarAccession.getDateUpdated().toGregorianCalendar().getTime());
                                        rca.setRecordStatus(pst.getReferenceClinVarAssertion().getRecordStatus());
                                        rca.setAssertionStatus(clinicalSignificanceType.getReviewStatus().value());
                                        rca.setAssertionType(pst.getReferenceClinVarAssertion().getAssertion().getType().value());
                                        rca.setAssertion(assertionRanking);
                                        rca.setTraitSet(traitSet);

                                        if ("GRCh38".equals(sequenceLocationType.getAssembly())) {

                                            GenomeRefSeq genomeRefSeq = all38GenomeRefSeqs.stream().filter(a -> a.getId().equals(accession))
                                                    .findAny().orElse(null);

                                            if (genomeRefSeq != null) {
                                                logger.debug(genomeRefSeq.toString());

                                                locatedVariant38 = LocatedVariantUtil.processMutation(measure, sequenceLocationType,
                                                        gerese4jBuild38, genomeRef38, genomeRefSeq, allVariantTypes);

                                                if (locatedVariant38 != null) {

                                                    List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                                                            .findByExample(locatedVariant38);
                                                    if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                                                        locatedVariant38
                                                                .setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant38));
                                                    } else {
                                                        locatedVariant38 = foundLocatedVariants.get(0);
                                                    }
                                                    logger.info(locatedVariant38.toString());
                                                    rca.setLocatedVariant(locatedVariant38);
                                                    rca.setId(canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca));
                                                    rca.getVersions().add(clinvarVersion);
                                                    canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca);
                                                    logger.info(rca.toString());

                                                    for (SubmissionClinicalAssertion sca : scaList) {
                                                        sca.setReferenceClinicalAssertion(rca);
                                                        canvasDAOBeanService.getSubmissionClinicalAssertionDAO().save(sca);
                                                    }

                                                }
                                            }
                                        }

                                        if ("GRCh37".equals(sequenceLocationType.getAssembly())) {

                                            GenomeRefSeq genomeRefSeq = all37GenomeRefSeqs.stream().filter(a -> a.getId().equals(accession))
                                                    .findAny().orElse(null);

                                            if (genomeRefSeq != null) {
                                                logger.debug(genomeRefSeq.toString());

                                                locatedVariant37 = LocatedVariantUtil.processMutation(measure, sequenceLocationType,
                                                        gerese4jBuild37, genomeRef37, genomeRefSeq, allVariantTypes);

                                                if (locatedVariant37 != null) {

                                                    List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                                                            .findByExample(locatedVariant37);
                                                    if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                                                        locatedVariant37
                                                                .setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant37));
                                                    } else {
                                                        locatedVariant37 = foundLocatedVariants.get(0);
                                                    }
                                                    logger.info(locatedVariant37.toString());
                                                    rca.setLocatedVariant(locatedVariant37);
                                                    rca.setId(canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca));
                                                    rca.getVersions().add(clinvarVersion);
                                                    canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca);
                                                    logger.info(rca.toString());

                                                    for (SubmissionClinicalAssertion sca : scaList) {
                                                        sca.setReferenceClinicalAssertion(rca);
                                                        canvasDAOBeanService.getSubmissionClinicalAssertionDAO().save(sca);
                                                    }

                                                }

                                            }

                                        }

                                    }

                                    canonicalLocatedVariants.add(Pair.of(locatedVariant37, locatedVariant38));

                                }

                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }

                    }
                });

            }

            es.shutdown();
            if (!es.awaitTermination(3L, TimeUnit.DAYS)) {
                es.shutdownNow();
            }

            FileUtils.deleteDirectory(clinvarDirTmp);

            canonicalize(canonicalLocatedVariants);

            UpdateDiagnosticResultVersionCallable callable = new UpdateDiagnosticResultVersionCallable(canvasDAOBeanService);
            callable.setNote(String.format("Persisted latest ClinVar: %s", clinvarVersion.getId()));
            Executors.newSingleThreadExecutor().submit(callable).get();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        long end = System.currentTimeMillis();
        logger.info("duration = {}", String.format("%s seconds", (end - start) / 1000D));

        return null;
    }

    private void canonicalize(List<Pair<LocatedVariant, LocatedVariant>> canonicalLocatedVariants) throws CANVASDAOException {

        logger.info("canonicalLocatedVariants.size(): {}", canonicalLocatedVariants.size());
        for (Pair<LocatedVariant, LocatedVariant> pair : canonicalLocatedVariants) {

            LocatedVariant locatedVariant37 = pair.getLeft();
            LocatedVariant locatedVariant38 = pair.getRight();

            if (locatedVariant37 == null && locatedVariant38 == null) {
                continue;
            }

            CanonicalAllele canonicalAllele = null;

            List<CanonicalAllele> foundCanonicalAllelesVia38 = new ArrayList<>();
            if (locatedVariant38 != null) {
                foundCanonicalAllelesVia38
                        .addAll(canvasDAOBeanService.getCanonicalAlleleDAO().findByLocatedVariantId(locatedVariant38.getId()));
            }

            List<CanonicalAllele> foundCanonicalAllelesVia37 = new ArrayList<>();
            if (locatedVariant37 != null) {
                foundCanonicalAllelesVia37
                        .addAll(canvasDAOBeanService.getCanonicalAlleleDAO().findByLocatedVariantId(locatedVariant37.getId()));
            }

            if (CollectionUtils.isEmpty(foundCanonicalAllelesVia37) && CollectionUtils.isEmpty(foundCanonicalAllelesVia38)) {

                Set<LocatedVariant> locatedVariants = new HashSet<>();
                if (locatedVariant37 != null) {
                    locatedVariants.add(locatedVariant37);
                }
                if (locatedVariant38 != null) {
                    locatedVariants.add(locatedVariant38);
                }
                if (!locatedVariants.isEmpty()) {
                    canonicalAllele = new CanonicalAllele();
                    canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                    canonicalAllele.getLocatedVariants().addAll(locatedVariants);
                    canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                }
            } else if (CollectionUtils.isNotEmpty(foundCanonicalAllelesVia37) && CollectionUtils.isEmpty(foundCanonicalAllelesVia38)
                    && locatedVariant38 != null) {

                canonicalAllele = foundCanonicalAllelesVia37.get(0);
                canonicalAllele.getLocatedVariants().add(locatedVariant38);
                canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);

            } else if (CollectionUtils.isEmpty(foundCanonicalAllelesVia37) && CollectionUtils.isNotEmpty(foundCanonicalAllelesVia38)
                    && locatedVariant37 != null) {

                canonicalAllele = foundCanonicalAllelesVia38.get(0);
                canonicalAllele.getLocatedVariants().add(locatedVariant37);
                canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);

            }

        }

    }

}
