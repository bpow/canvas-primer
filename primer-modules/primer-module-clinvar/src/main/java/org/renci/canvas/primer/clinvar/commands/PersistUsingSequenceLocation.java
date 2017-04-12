package org.renci.canvas.primer.clinvar.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
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
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinvar.model.ReferenceClinicalAssertion;
import org.renci.canvas.dao.clinvar.model.Trait;
import org.renci.canvas.dao.clinvar.model.TraitSet;
import org.renci.canvas.dao.clinvar.model.Version;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.CanonicalAllele;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.clinvar.ClinicalSignificanceType;
import org.renci.clinvar.MeasureSetType;
import org.renci.clinvar.MeasureSetType.Measure;
import org.renci.clinvar.MeasureSetType.Measure.AttributeSet;
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

            logger.info("finished download: {}", clinvarXmlFile.getAbsolutePath());
            Version clinvarVersion = new Version(clinvarXmlFile.getName());
            clinvarVersion.setId(canvasDAOBeanService.getVersionDAO().save(clinvarVersion));

            // List<String> schemaFileList = FTPFactory.ncbiListRemoteFiles("/pub/clinvar/xsd_public", "clinvar_public_", ".xsd");
            //
            // Pattern clinvarPublicFileNamePattern = Pattern.compile("clinvar_public_(?<v1>\\d+)\\.(?<v2>\\d+)\\.xsd");
            // List<Pair<Integer, Integer>> versionPairList = new ArrayList<>();
            // for (String schemaFile : schemaFileList) {
            // Matcher m = clinvarPublicFileNamePattern.matcher(schemaFile);
            // if (m.find()) {
            // String v1 = m.group("v1");
            // String v2 = m.group("v2");
            // versionPairList.add(Pair.of(Integer.valueOf(v1), Integer.valueOf(v2)));
            // }
            // }
            //
            // versionPairList.sort((a, b) -> {
            // int ret = b.getLeft().compareTo(a.getLeft());
            // if (ret == 0) {
            // ret = b.getRight().compareTo(a.getRight());
            // }
            // return ret;
            // });
            //
            // String clinvarXSDVersion = String.format("%s.%s", versionPairList.get(0).getLeft(), versionPairList.get(0).getRight());
            // String clinvarPublicBundleVersion = null;
            // for (Bundle bundle : Arrays.asList(bundleContext.getBundles())) {
            // if ("clinvar-public".equals(bundle.getSymbolicName())) {
            // clinvarPublicBundleVersion = bundle.getVersion().toString();
            // break;
            // }
            // }
            //
            // if (StringUtils.isNotEmpty(clinvarPublicBundleVersion) && !clinvarPublicBundleVersion.contains(clinvarXSDVersion)) {
            // logger.error("Rebuild https://github.com/jdr0887/clinvar_public using version: {}", clinvarXSDVersion);
            // return null;
            // }

            List<VariantType> allVariantTypes = canvasDAOBeanService.getVariantTypeDAO().findAll();

            List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();

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

                        if ("Variant".equals(measureSetType.getType())) {

                            List<Measure> measures = measureSetType.getMeasure();

                            if (CollectionUtils.isEmpty(measures)) {
                                continue;
                            }

                            for (Measure measure : measures) {

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

                                String measureType = measure.getType();
                                logger.info("measureType: {}", measureType);

                                List<SequenceLocationType> sequenceLocationTypeList = measure.getSequenceLocation();

                                Optional<SequenceLocationType> optionalSequenceLocationTypeFor38 = sequenceLocationTypeList.stream()
                                        .filter(a -> a.getStart() != null
                                                && (a.getVariantLength() != null && a.getVariantLength().intValue() < 100)
                                                && "GRCh38".equals(a.getAssembly()))
                                        .findAny();

                                LocatedVariant locatedVariant38 = null;

                                if (optionalSequenceLocationTypeFor38.isPresent()) {
                                    SequenceLocationType sequenceLocationType = optionalSequenceLocationTypeFor38.get();
                                    Optional<GenomeRef> foundGenomeRef = allGenomeRefs.stream()
                                            .filter(a -> a.getName().equals(gerese4jBuild38.getBuild().getVersion())).findAny();
                                    if (foundGenomeRef.isPresent()) {
                                        GenomeRef genomeRef = foundGenomeRef.get();
                                        locatedVariant38 = processMutation(measureType, sequenceLocationType, gerese4jBuild38, genomeRef,
                                                allVariantTypes);
                                    }
                                }

                                if (locatedVariant38 != null) {
                                    persistAssertion(locatedVariant38, rat);
                                }

                                Optional<SequenceLocationType> optionalSequenceLocationTypeFor37 = sequenceLocationTypeList.stream()
                                        .filter(a -> a.getStart() != null
                                                && (a.getVariantLength() != null && a.getVariantLength().intValue() < 100)
                                                && "GRCh37".equals(a.getAssembly()))
                                        .findAny();

                                LocatedVariant locatedVariant37 = null;

                                if (optionalSequenceLocationTypeFor37.isPresent()) {
                                    SequenceLocationType sequenceLocationType = optionalSequenceLocationTypeFor37.get();
                                    Optional<GenomeRef> foundGenomeRef = allGenomeRefs.stream()
                                            .filter(a -> a.getName().equals(gerese4jBuild37.getBuild().getVersion())).findAny();
                                    if (foundGenomeRef.isPresent()) {
                                        GenomeRef genomeRef = foundGenomeRef.get();
                                        locatedVariant37 = processMutation(measureType, sequenceLocationType, gerese4jBuild37, genomeRef,
                                                allVariantTypes);
                                    }
                                }

                                if (locatedVariant37 != null) {
                                    persistAssertion(locatedVariant37, rat);
                                }

                                CanonicalAllele canonicalAllele = null;

                                List<CanonicalAllele> foundCanonicalAllelesVia38 = new ArrayList<>();
                                if (locatedVariant38 != null) {
                                    foundCanonicalAllelesVia38.addAll(
                                            canvasDAOBeanService.getCanonicalAlleleDAO().findByLocatedVariantId(locatedVariant38.getId()));
                                }

                                List<CanonicalAllele> foundCanonicalAllelesVia37 = new ArrayList<>();
                                if (locatedVariant37 != null) {
                                    foundCanonicalAllelesVia37.addAll(
                                            canvasDAOBeanService.getCanonicalAlleleDAO().findByLocatedVariantId(locatedVariant37.getId()));
                                }

                                if (CollectionUtils.isEmpty(foundCanonicalAllelesVia37)
                                        && CollectionUtils.isEmpty(foundCanonicalAllelesVia38)) {

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

                                if (CollectionUtils.isNotEmpty(foundCanonicalAllelesVia37)
                                        && CollectionUtils.isEmpty(foundCanonicalAllelesVia38)) {
                                    canonicalAllele = foundCanonicalAllelesVia37.get(0);
                                    canonicalAllele.getLocatedVariants().add(locatedVariant38);
                                    canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                                }

                                if (CollectionUtils.isEmpty(foundCanonicalAllelesVia37)
                                        && CollectionUtils.isNotEmpty(foundCanonicalAllelesVia38)) {
                                    canonicalAllele = foundCanonicalAllelesVia38.get(0);
                                    canonicalAllele.getLocatedVariants().add(locatedVariant37);
                                    canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                                }

                            }

                        }
                    } else {
                        reader.next();
                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        long end = System.currentTimeMillis();
        logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000));

        return null;
    }

    private void persistAssertion(LocatedVariant locatedVariant, ReferenceAssertionType rat) {

        try {
            ClinVarAccession clinvarAccession = rat.getClinVarAccession();
            TraitSetType traitSetType = rat.getTraitSet();

            ReferenceClinicalAssertion rca = new ReferenceClinicalAssertion();
            rca.setAccession(clinvarAccession.getAcc());
            rca.setRecordStatus(rat.getRecordStatus());
            rca.setVersion(clinvarAccession.getVersion().intValue());
            rca.setCreated(new java.sql.Date(rat.getDateCreated().toGregorianCalendar().getTimeInMillis()));
            rca.setUpdated(new java.sql.Date(clinvarAccession.getDateUpdated().toGregorianCalendar().getTimeInMillis()));
            rca.setLocatedVariant(locatedVariant);

            ClinicalSignificanceType clinicalSignificanceType = rat.getClinicalSignificance();
            rca.setAssertionStatus(clinicalSignificanceType.getReviewStatus().value());
            rca.setAssertion(clinicalSignificanceType.getDescription());
            rca.setAssertionType(rat.getAssertion().getType().value());

            TraitSet cTraitSet = canvasDAOBeanService.getTraitSetDAO().findById(traitSetType.getID().intValue());
            if (cTraitSet == null) {
                cTraitSet = new TraitSet(traitSetType.getID().intValue(), traitSetType.getType());
                logger.info(cTraitSet.toString());
                canvasDAOBeanService.getTraitSetDAO().save(cTraitSet);
            }

            List<TraitType> traitTypeList = traitSetType.getTrait();

            for (TraitType traitType : traitTypeList) {

                Trait cTrait = canvasDAOBeanService.getTraitDAO().findById(traitType.getID().intValue());
                if (cTrait == null) {
                    Optional<SetElementSetType> preferredNameOptional = traitType.getName().stream()
                            .filter(a -> a.getElementValue().getType().equals("Preferred")).findAny();
                    if (preferredNameOptional.isPresent()) {
                        cTrait = new Trait(traitType.getID().intValue(), traitType.getType(),
                                preferredNameOptional.get().getElementValue().getValue());
                        logger.info(cTrait.toString());
                        canvasDAOBeanService.getTraitSetDAO().save(cTraitSet);
                        cTraitSet.getTraits().add(cTrait);
                    }
                }

            }

            canvasDAOBeanService.getTraitSetDAO().save(cTraitSet);
            rca.setTraitSet(cTraitSet);
            canvasDAOBeanService.getReferenceClinicalAssertionDAO().save(rca);
            logger.info(rca.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private LocatedVariant processMutation(String measureType, SequenceLocationType sequenceLocationType, GeReSe4jBuild gerese4jBuild,
            GenomeRef genomeRef, List<VariantType> allVariantTypes) {
        LocatedVariant locatedVariant = null;
        String refBase = null;

        switch (measureType) {
            case "Deletion":

                try {

                    if (sequenceLocationType.getStart().intValue() == sequenceLocationType.getStop().intValue()) {
                        refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(),
                                true);
                    } else {
                        refBase = gerese4jBuild.getRegion(sequenceLocationType.getAccession(),
                                Range.between(sequenceLocationType.getStart().intValue(), sequenceLocationType.getStop().intValue()), true);
                    }

                    locatedVariant = persistDeletion(allVariantTypes.stream().filter(a -> a.getId().equals("del")).findAny().get(),
                            genomeRef, sequenceLocationType, refBase);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                break;
            case "Insertion":
                try {
                    if (sequenceLocationType.getStart().intValue() == sequenceLocationType.getStop().intValue()) {
                        refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(),
                                true);
                    } else {
                        refBase = gerese4jBuild.getRegion(sequenceLocationType.getAccession(),
                                Range.between(sequenceLocationType.getStart().intValue(), sequenceLocationType.getStop().intValue()), true);
                    }

                    locatedVariant = persistInsertion(allVariantTypes.stream().filter(a -> a.getId().equals("ins")).findAny().get(),
                            genomeRef, sequenceLocationType, refBase);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                break;
            case "Duplication":
                try {
                    if (sequenceLocationType.getStart().intValue() == sequenceLocationType.getStop().intValue()) {
                        refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(),
                                true);
                    } else {
                        refBase = gerese4jBuild.getRegion(sequenceLocationType.getAccession(),
                                Range.between(sequenceLocationType.getStart().intValue(), sequenceLocationType.getStop().intValue()), true);
                    }

                    locatedVariant = persistDuplication(allVariantTypes.stream().filter(a -> a.getId().equals("ins")).findAny().get(),
                            genomeRef, sequenceLocationType, refBase);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                break;
            case "single nucleotide variant":

                try {
                    refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(), true);
                    locatedVariant = persistSNP(allVariantTypes.stream().filter(a -> a.getId().equals("snp")).findAny().get(), genomeRef,
                            sequenceLocationType, refBase);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                break;
        }

        return locatedVariant;
    }

    private LocatedVariant persistDeletion(VariantType variantType, GenomeRef genomeRef, SequenceLocationType slt, String refBase) {
        logger.debug("ENTERING persistDeletion(VariantType, GenomeRef, SequenceLocationType, String, ReferenceAssertionType)");
        LocatedVariant locatedVariant = null;
        try {

            GenomeRefSeq genomeRefSeq = canvasDAOBeanService.getGenomeRefSeqDAO().findById(slt.getAccession());
            logger.info(genomeRefSeq.toString());

            locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
            locatedVariant.setVariantType(variantType);
            locatedVariant.setSeq(refBase);
            locatedVariant.setRef(refBase);
            locatedVariant.setPosition(slt.getStart().intValue());
            locatedVariant.setEndPosition(slt.getStart().intValue() + slt.getVariantLength().intValue());

            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findByExample(locatedVariant);
            if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                locatedVariant.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
            } else {
                locatedVariant = foundLocatedVariants.get(0);
            }
            logger.info(locatedVariant.toString());

        } catch (CANVASDAOException e) {
            logger.error(e.getMessage(), e);
        }

        return locatedVariant;
    }

    private LocatedVariant persistInsertion(VariantType variantType, GenomeRef genomeRef, SequenceLocationType slt, String refBase) {
        logger.debug("ENTERING persistInsertion(VariantType, GenomeRef, SequenceLocationType, String)");
        LocatedVariant locatedVariant = null;

        try {

            GenomeRefSeq genomeRefSeq = canvasDAOBeanService.getGenomeRefSeqDAO().findById(slt.getAccession());
            logger.info(genomeRefSeq.toString());

            locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
            locatedVariant.setVariantType(variantType);

            String ref = refBase;
            char[] referenceChars = ref.toCharArray();

            String alt = slt.getAlternateAllele();
            char[] alternateChars = alt.toCharArray();

            StringBuilder charsToRemove = new StringBuilder();

            for (int i = 0; i < referenceChars.length; ++i) {
                if (referenceChars[i] != alternateChars[i]) {
                    break;
                }
                charsToRemove.append(referenceChars[i]);
            }
            if (charsToRemove.length() > 0) {
                // remove from front
                locatedVariant.setPosition(slt.getStart().intValue() + charsToRemove.length());
                locatedVariant.setSeq(alt.replaceFirst(charsToRemove.toString(), ""));
            }
            // } else {
            // // remove from back
            // for (int i = referenceChars.length - 1; i > 0; --i) {
            // if (referenceChars[i] != alternateChars[i]) {
            // break;
            // }
            // charsToRemove.append(referenceChars[i]);
            // }
            //
            // if (charsToRemove.length() > 0) {
            // charsToRemove.reverse();
            // locatedVariant.setPosition(slt.getStart().intValue());
            // locatedVariant.setSeq(StringUtils.removeEnd(alt, charsToRemove.toString()));
            // }
            // }
            locatedVariant.setEndPosition(locatedVariant.getPosition() + 1);

            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findByExample(locatedVariant);
            if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                locatedVariant.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
            } else {
                locatedVariant = foundLocatedVariants.get(0);
            }
            logger.info(locatedVariant.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return locatedVariant;
    }

    private LocatedVariant persistDuplication(VariantType variantType, GenomeRef genomeRef, SequenceLocationType slt, String refBase) {
        logger.debug("ENTERING persistDuplication(VariantType, GenomeRef, SequenceLocationType, String)");
        LocatedVariant locatedVariant = null;

        try {

            GenomeRefSeq genomeRefSeq = canvasDAOBeanService.getGenomeRefSeqDAO().findById(slt.getAccession());
            logger.info(genomeRefSeq.toString());

            locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
            locatedVariant.setVariantType(variantType);

            String ref = refBase;
            char[] referenceChars = ref.toCharArray();

            if (StringUtils.isEmpty(slt.getAlternateAllele()) && slt.getVariantLength() != null) {
                locatedVariant.setPosition(slt.getStop().intValue());
                locatedVariant.setSeq(refBase);
            }

            if (StringUtils.isNotEmpty(slt.getAlternateAllele())) {
                String alt = slt.getAlternateAllele();
                char[] alternateChars = alt.toCharArray();

                StringBuilder charsToRemove = new StringBuilder();

                for (int i = 0; i < referenceChars.length; ++i) {
                    if (referenceChars[i] != alternateChars[i]) {
                        break;
                    }
                    charsToRemove.append(referenceChars[i]);
                }
                if (charsToRemove.length() > 0) {
                    // remove from front
                    locatedVariant.setPosition(slt.getStart().intValue() + charsToRemove.length());
                    locatedVariant.setSeq(alt.replaceFirst(charsToRemove.toString(), ""));
                }
            }

            locatedVariant.setEndPosition(locatedVariant.getPosition() + 1);

            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findByExample(locatedVariant);
            if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                locatedVariant.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
            } else {
                locatedVariant = foundLocatedVariants.get(0);
            }
            logger.info(locatedVariant.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return locatedVariant;
    }

    private LocatedVariant persistSNP(VariantType variantType, GenomeRef genomeRef, SequenceLocationType slt, String refBase) {
        logger.debug("ENTERING persistSNP(VariantType, GenomeRef, SequenceLocationType, String, ReferenceAssertionType)");

        LocatedVariant locatedVariant = null;
        try {

            GenomeRefSeq genomeRefSeq = canvasDAOBeanService.getGenomeRefSeqDAO().findById(slt.getAccession());
            logger.info(genomeRefSeq.toString());

            locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
            locatedVariant.setPosition(slt.getStart().intValue());
            locatedVariant.setEndPosition(slt.getStart().intValue() + 1);
            locatedVariant.setVariantType(variantType);
            locatedVariant.setRef(refBase);
            locatedVariant.setSeq(slt.getAlternateAllele());

            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findByExample(locatedVariant);
            if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                locatedVariant.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
            } else {
                locatedVariant = foundLocatedVariants.get(0);
            }
            logger.info(locatedVariant.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return locatedVariant;
    }

}
