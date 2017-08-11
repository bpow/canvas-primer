package org.renci.canvas.primer.clinvar.commands;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinvar.model.AssertionRanking;
import org.renci.canvas.dao.clinvar.model.ClinVarVersion;
import org.renci.canvas.dao.clinvar.model.ReferenceClinicalAssertion;
import org.renci.canvas.dao.clinvar.model.Trait;
import org.renci.canvas.dao.clinvar.model.TraitSet;
import org.renci.canvas.dao.commons.LocatedVariantFactory;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.CanonicalAllele;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.canvas.primer.commons.UpdateDiagnosticResultVersionCallable;
import org.renci.clinvar.ClinicalSignificanceType;
import org.renci.clinvar.MeasureSetType;
import org.renci.clinvar.MeasureType;
import org.renci.clinvar.MeasureType.AttributeSet;
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
            GeReSe4jBuild gerese4jBuild38 = GeReSe4jBuild_38_7.getInstance();

            Path clinvarPath = Paths.get(System.getProperty("karaf.data"), "ClinVar");
            File clinvarDir = clinvarPath.toFile();
            if (!clinvarDir.exists()) {
                clinvarDir.mkdirs();
            }

            File clinvarXmlFile = FTPFactory.ncbiDownload(clinvarDir, "/pub/clinvar/xml", "ClinVarFullRelease_00-latest.xml.gz");

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

            List<ReferenceAssertionType> rats = new ArrayList<>();

            logger.info("parsing: {}", clinvarXmlFile.getName());

            try (FileInputStream fis = new FileInputStream(clinvarXmlFile);
                    GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue())) {

                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                XMLEventReader reader = xmlInputFactory.createXMLEventReader(gzis);

                JAXBContext jc = JAXBContext.newInstance(ReleaseType.class, ReferenceAssertionType.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();

                QName qName = new QName("ReferenceClinVarAssertion");

                // ReleaseType releaseType = unmarshaller.unmarshal(new PartialXmlEventReader(reader, qName), ReleaseType.class).getValue();

                XMLEvent xmlEvent = null;
                while ((xmlEvent = reader.peek()) != null) {

                    if (xmlEvent.isStartElement() && ((StartElement) xmlEvent).getName().equals(qName)) {

                        ReferenceAssertionType rat = unmarshaller.unmarshal(reader, ReferenceAssertionType.class).getValue();

                        MeasureSetType measureSetType = rat.getMeasureSet();

                        if (measureSetType != null && "Variant".equals(measureSetType.getType())) {

                            List<MeasureType> measures = measureSetType.getMeasure();

                            if (CollectionUtils.isEmpty(measures)) {
                                continue;
                            }

                            for (MeasureType measure : measures) {

                                List<AttributeSet> filters = measure.getAttributeSet().stream()
                                        .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level"))
                                        .collect(Collectors.toList());

                                if (CollectionUtils.isEmpty(filters)) {
                                    continue;
                                }

                                filters = measure.getAttributeSet().stream()
                                        .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level")
                                                && a.getAttribute().getValue().contains("?"))
                                        .collect(Collectors.toList());
                                // filter on HGVS expressions that can't be explicitly resolved regardless of type
                                if (CollectionUtils.isNotEmpty(filters)) {
                                    continue;
                                }

                                rats.add(rat);
                            }

                        }
                    } else {
                        reader.next();
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            logger.info("rats.size(): {}", rats.size());

            logger.info("persisting AssertionRankings");
            List<String> clinicalSignificanceDescList = rats.parallelStream().map(a -> a.getClinicalSignificance().getDescription())
                    .distinct().collect(Collectors.toList());
            for (String clinicalSignificanceDesc : clinicalSignificanceDescList) {
                AssertionRanking assertionRanking = canvasDAOBeanService.getAssertionRankingDAO().findById(clinicalSignificanceDesc);
                if (assertionRanking == null) {
                    assertionRanking = new AssertionRanking(clinicalSignificanceDesc);
                    canvasDAOBeanService.getAssertionRankingDAO().save(assertionRanking);
                }
            }

            logger.info("persisting TraitSets");
            Set<TraitSet> traitSets = new HashSet<>();
            for (ReferenceAssertionType rat : rats) {
                traitSets.add(new TraitSet(rat.getTraitSet().getID().intValue(), rat.getTraitSet().getType()));
            }

            ExecutorService es = Executors.newFixedThreadPool(3);
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

            Set<Trait> traits = new HashSet<>();

            logger.info("persisting Traits");
            for (ReferenceAssertionType rat : rats) {
                List<TraitType> traitTypeList = rat.getTraitSet().getTrait();
                for (TraitType traitType : traitTypeList) {
                    Optional<SetElementSetType> preferredNameOptional = traitType.getName().stream()
                            .filter(b -> b.getElementValue().getType().equals("Preferred")).findAny();
                    if (preferredNameOptional.isPresent()) {
                        traits.add(new Trait(traitType.getID().intValue(), traitType.getType(),
                                preferredNameOptional.get().getElementValue().getValue()));
                    }
                }
            }

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
            for (ReferenceAssertionType rat : rats) {
                es.submit(() -> {
                    try {
                        List<TraitType> traitTypeList = rat.getTraitSet().getTrait();
                        for (TraitType traitType : traitTypeList) {
                            Optional<SetElementSetType> preferredNameOptional = traitType.getName().stream()
                                    .filter(b -> b.getElementValue().getType().equals("Preferred")).findAny();
                            if (preferredNameOptional.isPresent()) {
                                Trait trait = canvasDAOBeanService.getTraitDAO().findById(traitType.getID().intValue());
                                TraitSet traitSet = canvasDAOBeanService.getTraitSetDAO().findById(rat.getTraitSet().getID().intValue());
                                if (!traitSet.getTraits().contains(trait)) {
                                    traitSet.getTraits().add(trait);
                                    canvasDAOBeanService.getTraitSetDAO().save(traitSet);
                                }
                            }
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

            List<AssertionRanking> allAssertionRankings = canvasDAOBeanService.getAssertionRankingDAO().findAll();

            List<Pair<LocatedVariant, LocatedVariant>> canonicalLocatedVariants = new ArrayList<>();

            List<ReferenceClinicalAssertion> referenceClinicalAssertions = new ArrayList<>();

            for (ReferenceAssertionType rat : rats) {

                try {
                    MeasureSetType measureSetType = rat.getMeasureSet();

                    ClinicalSignificanceType clinicalSignificanceType = rat.getClinicalSignificance();
                    ClinVarAccession clinvarAccession = rat.getClinVarAccession();

                    AssertionRanking assertionRanking = allAssertionRankings.stream()
                            .filter(a -> a.getId().equals(clinicalSignificanceType.getDescription())).findFirst().get();

                    TraitSetType traitSetType = rat.getTraitSet();
                    TraitSet traitSet = canvasDAOBeanService.getTraitSetDAO().findById(traitSetType.getID().intValue());

                    if (measureSetType != null && "Variant".equals(measureSetType.getType())) {

                        List<MeasureType> measureTypes = measureSetType.getMeasure();

                        for (MeasureType measureType : measureTypes) {

                            String measure = measureType.getType();
                            logger.debug("measure: {}", measure);

                            List<SequenceLocationType> sequenceLocationTypeList = measureType.getSequenceLocation();

                            LocatedVariant locatedVariant38 = null;
                            for (SequenceLocationType sequenceLocationType : sequenceLocationTypeList) {

                                if (sequenceLocationType.getStart() != null
                                        && (sequenceLocationType.getVariantLength() != null
                                                && sequenceLocationType.getVariantLength().intValue() < 100)
                                        && "GRCh38".equals(sequenceLocationType.getAssembly())) {

                                    String accession = sequenceLocationType.getAccession();

                                    GenomeRefSeq genomeRefSeq = all38GenomeRefSeqs.parallelStream().filter(a -> a.getId().equals(accession))
                                            .findFirst().orElse(null);

                                    if (genomeRefSeq != null) {

                                        logger.debug(genomeRefSeq.toString());

                                        locatedVariant38 = processMutation(measure, sequenceLocationType, gerese4jBuild38, genomeRef38,
                                                genomeRefSeq, allVariantTypes);

                                        if (locatedVariant38 != null) {

                                            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                                                    .findByExample(locatedVariant38);
                                            if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                                                locatedVariant38.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant38));
                                            } else {
                                                locatedVariant38 = foundLocatedVariants.get(0);
                                            }
                                            logger.info(locatedVariant38.toString());

                                            ReferenceClinicalAssertion rca = new ReferenceClinicalAssertion(clinvarAccession.getAcc(),
                                                    clinvarAccession.getVersion().intValue(),
                                                    new java.sql.Date(rat.getDateCreated().toGregorianCalendar().getTimeInMillis()),
                                                    new java.sql.Date(
                                                            clinvarAccession.getDateUpdated().toGregorianCalendar().getTimeInMillis()),
                                                    rat.getRecordStatus(), clinicalSignificanceType.getReviewStatus().value(),
                                                    rat.getAssertion().getType().value());

                                            rca.setAssertion(assertionRanking);
                                            rca.setLocatedVariant(locatedVariant38);
                                            rca.setTraitSet(traitSet);

                                            referenceClinicalAssertions.add(rca);

                                        }
                                    }
                                    break;
                                }

                            }

                            LocatedVariant locatedVariant37 = null;
                            for (SequenceLocationType sequenceLocationType : sequenceLocationTypeList) {

                                if (sequenceLocationType.getStart() != null
                                        && (sequenceLocationType.getVariantLength() != null
                                                && sequenceLocationType.getVariantLength().intValue() < 100)
                                        && "GRCh37".equals(sequenceLocationType.getAssembly())) {

                                    String accession = sequenceLocationType.getAccession();

                                    GenomeRefSeq genomeRefSeq = all37GenomeRefSeqs.parallelStream().filter(a -> a.getId().equals(accession))
                                            .findFirst().orElse(null);

                                    if (genomeRefSeq != null) {

                                        logger.debug(genomeRefSeq.toString());

                                        locatedVariant37 = processMutation(measure, sequenceLocationType, gerese4jBuild37, genomeRef37,
                                                genomeRefSeq, allVariantTypes);
                                        if (locatedVariant37 != null) {

                                            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                                                    .findByExample(locatedVariant37);
                                            if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                                                locatedVariant37.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant37));
                                            } else {
                                                locatedVariant37 = foundLocatedVariants.get(0);
                                            }
                                            logger.info(locatedVariant37.toString());

                                            ReferenceClinicalAssertion rca = new ReferenceClinicalAssertion(clinvarAccession.getAcc(),
                                                    clinvarAccession.getVersion().intValue(),
                                                    new java.sql.Date(rat.getDateCreated().toGregorianCalendar().getTimeInMillis()),
                                                    new java.sql.Date(
                                                            clinvarAccession.getDateUpdated().toGregorianCalendar().getTimeInMillis()),
                                                    rat.getRecordStatus(), clinicalSignificanceType.getReviewStatus().value(),
                                                    rat.getAssertion().getType().value());

                                            rca.setAssertion(assertionRanking);
                                            rca.setLocatedVariant(locatedVariant37);
                                            rca.setTraitSet(traitSet);

                                            referenceClinicalAssertions.add(rca);

                                        }

                                    }

                                    break;
                                }

                            }

                            canonicalLocatedVariants.add(Pair.of(locatedVariant37, locatedVariant38));

                        }

                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

            }

            logger.info("referenceClinicalAssertions.size(): {}", referenceClinicalAssertions.size());

            logger.info("persisting ReferenceClinicalAssertions");
            es = Executors.newFixedThreadPool(4);
            for (ReferenceClinicalAssertion rca : referenceClinicalAssertions) {
                es.submit(() -> {
                    try {
                        rca.setId(canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca));
                        rca.getVersions().add(clinvarVersion);
                        canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca);
                        logger.info(rca.toString());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
            es.shutdown();
            if (!es.awaitTermination(1L, TimeUnit.DAYS)) {
                es.shutdownNow();
            }
            referenceClinicalAssertions.clear();

            logger.info("canonicalLocatedVariants.size(): {}", canonicalLocatedVariants.size());

            logger.info("canonicalizing");
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
                }

                if (CollectionUtils.isNotEmpty(foundCanonicalAllelesVia37) && CollectionUtils.isEmpty(foundCanonicalAllelesVia38)
                        && locatedVariant38 != null) {
                    canonicalAllele = foundCanonicalAllelesVia37.get(0);
                    canonicalAllele.getLocatedVariants().add(locatedVariant38);
                    canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                }

                if (CollectionUtils.isEmpty(foundCanonicalAllelesVia37) && CollectionUtils.isNotEmpty(foundCanonicalAllelesVia38)
                        && locatedVariant37 != null) {
                    canonicalAllele = foundCanonicalAllelesVia38.get(0);
                    canonicalAllele.getLocatedVariants().add(locatedVariant37);
                    canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                }

            }

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

    private LocatedVariant processMutation(String measureType, SequenceLocationType sequenceLocationType, GeReSe4jBuild gerese4jBuild,
            GenomeRef genomeRef, GenomeRefSeq genomeRefSeq, List<VariantType> allVariantTypes) {
        LocatedVariant locatedVariant = null;
        String refBase = null;

        try {
            switch (measureType) {
                case "Deletion":

                    if (sequenceLocationType.getStart().intValue() == sequenceLocationType.getStop().intValue()) {
                        refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(),
                                true);
                    } else {
                        refBase = gerese4jBuild.getRegion(sequenceLocationType.getAccession(),
                                Range.between(sequenceLocationType.getStart().intValue(), sequenceLocationType.getStop().intValue()), true);
                    }

                    locatedVariant = LocatedVariantFactory.createDeletion(genomeRef, genomeRefSeq,
                            allVariantTypes.stream().filter(a -> a.getId().equals("del")).findAny().get(), refBase,
                            sequenceLocationType.getAlternateAllele(), sequenceLocationType.getStart().intValue());

                    break;
                case "Insertion":
                case "Duplication":

                    if (sequenceLocationType.getStart().intValue() == sequenceLocationType.getStop().intValue()) {
                        refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(),
                                true);
                    } else {
                        refBase = gerese4jBuild.getRegion(sequenceLocationType.getAccession(),
                                Range.between(sequenceLocationType.getStart().intValue(), sequenceLocationType.getStop().intValue()), true);
                    }

                    if (StringUtils.isNotEmpty(sequenceLocationType.getAlternateAllele())) {
                        locatedVariant = LocatedVariantFactory.createInsertion(genomeRef, genomeRefSeq,
                                allVariantTypes.stream().filter(a -> a.getId().equals("ins")).findAny().get(), refBase,
                                sequenceLocationType.getAlternateAllele(), sequenceLocationType.getStart().intValue());
                    }

                    break;
                case "single nucleotide variant":

                    refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(), true);

                    if (StringUtils.isNotEmpty(sequenceLocationType.getAlternateAllele())) {
                        locatedVariant = LocatedVariantFactory.createSNP(genomeRef, genomeRefSeq,
                                allVariantTypes.stream().filter(a -> a.getId().equals("snp")).findAny().get(), refBase,
                                sequenceLocationType.getAlternateAllele(), sequenceLocationType.getStart().intValue());
                    }

                    break;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return locatedVariant;
    }

}
