package org.renci.canvas.primer.clinvar.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinvar.model.ReferenceClinicalAssertion;
import org.renci.canvas.dao.clinvar.model.Trait;
import org.renci.canvas.dao.clinvar.model.TraitSet;
import org.renci.canvas.dao.clinvar.model.Version;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.clinvar.ClinicalSignificanceType;
import org.renci.clinvar.MeasureSetType;
import org.renci.clinvar.MeasureSetType.Measure;
import org.renci.clinvar.MeasureSetType.Measure.AttributeSet;
import org.renci.clinvar.PublicSetType;
import org.renci.clinvar.ReferenceAssertionType;
import org.renci.clinvar.ReferenceAssertionType.ClinVarAccession;
import org.renci.clinvar.ReleaseType;
import org.renci.clinvar.SetElementSetType;
import org.renci.clinvar.TraitSetType;
import org.renci.clinvar.TraitType;
import org.renci.hgvs.HGVSParser;
import org.renci.hgvs.model.dna.DNAVariantMutation;
import org.renci.hgvs.model.dna.DeletionAlleleInfo;
import org.renci.hgvs.model.dna.DuplicationAlleleInfo;
import org.renci.hgvs.model.dna.InsertionAlleleInfo;
import org.renci.hgvs.model.dna.SubstitutionAlleleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistUsingHGVS implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(PersistUsingHGVS.class);

    private static final List<String> allowedTypes = Arrays.asList("single nucleotide variant", "Duplication", "Deletion", "Indel",
            "inversion");

    private CANVASDAOBeanService canvasDAOBeanService;

    private BundleContext bundleContext;

    public PersistUsingHGVS(CANVASDAOBeanService canvasDAOBeanService, BundleContext bundleContext) {
        super();
        this.canvasDAOBeanService = canvasDAOBeanService;
        this.bundleContext = bundleContext;
    }

    @Override
    public Void call() {
        logger.debug("ENTERING run()");

        try {
            String date = FastDateFormat.getInstance("yyyy-MM").format(new Date());

            Path outputPath = Paths.get(System.getProperty("karaf.data"), "tmp", "clinvar");
            File outputDir = outputPath.toFile();
            outputDir.mkdirs();

            File clinvarXmlFile = FTPFactory.ncbiDownload(outputDir, "/pub/clinvar/xml",
                    String.format("ClinVarFullRelease_%s.xml.gz", date));

            Version clinvarVersion = new Version(clinvarXmlFile.getName());
            clinvarVersion.setId(canvasDAOBeanService.getVersionDAO().save(clinvarVersion));

            List<String> schemaFileList = FTPFactory.ncbiListRemoteFiles("/pub/clinvar/xsd_public", "clinvar_public_", ".xsd");

            Pattern clinvarPublicFileNamePattern = Pattern.compile("clinvar_public_(?<v1>\\d+)\\.(?<v2>\\d+)\\.xsd");
            List<Pair<Integer, Integer>> versionPairList = new ArrayList<>();
            for (String schemaFile : schemaFileList) {
                Matcher m = clinvarPublicFileNamePattern.matcher(schemaFile);
                if (m.find()) {
                    String v1 = m.group("v1");
                    String v2 = m.group("v2");
                    versionPairList.add(Pair.of(Integer.valueOf(v1), Integer.valueOf(v2)));
                }
            }

            versionPairList.sort((a, b) -> {
                int ret = b.getLeft().compareTo(a.getLeft());
                if (ret == 0) {
                    ret = b.getRight().compareTo(a.getRight());
                }
                return ret;
            });

            String clinvarXSDVersion = String.format("%s.%s", versionPairList.get(0).getLeft(), versionPairList.get(0).getRight());
            String clinvarPublicBundleVersion = null;
            for (Bundle bundle : Arrays.asList(bundleContext.getBundles())) {
                if ("clinvar-public".equals(bundle.getSymbolicName())) {
                    clinvarPublicBundleVersion = bundle.getVersion().toString();
                    break;
                }
            }

            if (StringUtils.isNotEmpty(clinvarPublicBundleVersion) && !clinvarPublicBundleVersion.contains(clinvarXSDVersion)) {
                logger.error("Rebuild https://github.com/jdr0887/clinvar_public using version: {}", clinvarXSDVersion);
                return null;
            }

            List<VariantType> allVariantTypes = canvasDAOBeanService.getVariantTypeDAO().findAll();
            List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();

            try (FileInputStream fis = new FileInputStream(clinvarXmlFile); GZIPInputStream gzis = new GZIPInputStream(fis)) {

                XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(gzis);

                JAXBContext jc = JAXBContext.newInstance(ReleaseType.class);
                Unmarshaller u = jc.createUnmarshaller();
                JAXBElement<ReleaseType> releaseType = u.unmarshal(reader, ReleaseType.class);
                List<PublicSetType> publicSetTypeList = releaseType.getValue().getClinVarSet();

                for (PublicSetType pst : publicSetTypeList) {

                    try {
                        ReferenceAssertionType rat = pst.getReferenceClinVarAssertion();

                        MeasureSetType measureSetType = rat.getMeasureSet();
                        if ("Variant".equals(measureSetType.getType())) {
                            List<Measure> measures = measureSetType.getMeasure();

                            if (CollectionUtils.isNotEmpty(measures)) {

                                for (Measure measure : measures) {

                                    List<AttributeSet> attributeSets = measure.getAttributeSet().stream()
                                            .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level")
                                                    && a.getAttribute().getValue().startsWith("NC_")
                                                    && a.getAttribute().getIntegerValue() != 36
                                                    && !a.getAttribute().getValue().contains("?"))
                                            .collect(Collectors.toList());

                                    if (CollectionUtils.isNotEmpty(attributeSets)) {
                                        for (AttributeSet attributeSet : attributeSets) {

                                            GenomeRefSeq genomeRefSeq = canvasDAOBeanService.getGenomeRefSeqDAO()
                                                    .findById(String.format("%s.%s", attributeSet.getAttribute().getAccession(),
                                                            attributeSet.getAttribute().getVersion().toString()));

                                            if (genomeRefSeq != null) {

                                                GenomeRef genomeRef = null;
                                                switch (attributeSet.getAttribute().getIntegerValue()) {
                                                    case 38:
                                                        genomeRef = allGenomeRefs.stream().filter(a -> a.getId().equals(4)).findFirst()
                                                                .get();
                                                        break;
                                                    case 37:
                                                    default:
                                                        genomeRef = allGenomeRefs.stream().filter(a -> a.getId().equals(2)).findFirst()
                                                                .get();
                                                        break;
                                                }

                                                LocatedVariant locatedVariant = null;
                                                HGVSParser hgvsParser = HGVSParser.getInstance();
                                                DNAVariantMutation change = hgvsParser
                                                        .parseDNAMutation(attributeSet.getAttribute().getValue());

                                                switch (change.getChangeType()) {
                                                    case DELETION:
                                                        locatedVariant = parseDeletion(genomeRef, genomeRefSeq, allVariantTypes,
                                                                (DeletionAlleleInfo) change.getAlleleInfo());
                                                        break;
                                                    case DUPLICATION:
                                                        locatedVariant = parseDuplication(genomeRef, genomeRefSeq, allVariantTypes,
                                                                (DuplicationAlleleInfo) change.getAlleleInfo());
                                                        break;
                                                    case INSERTION:
                                                        locatedVariant = parseInsertion(genomeRef, genomeRefSeq, allVariantTypes,
                                                                (InsertionAlleleInfo) change.getAlleleInfo());
                                                        break;
                                                    case SUBSTITUTION:
                                                        locatedVariant = parseSubstitution(genomeRef, genomeRefSeq, allVariantTypes,
                                                                (SubstitutionAlleleInfo) change.getAlleleInfo());
                                                        break;
                                                }

                                                if (locatedVariant != null) {
                                                    List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                                                            .findByExample(locatedVariant);
                                                    if (CollectionUtils.isNotEmpty(foundLocatedVariants)) {
                                                        locatedVariant = foundLocatedVariants.get(0);
                                                    } else {
                                                        locatedVariant
                                                                .setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
                                                    }
                                                }

                                            }

                                        }
                                    }

                                }

                            }

                        }

                        ClinVarAccession clinvarAccession = rat.getClinVarAccession();

                        ReferenceClinicalAssertion rca = new ReferenceClinicalAssertion();
                        rca.setAccession(clinvarAccession.getAcc());
                        rca.setRecordStatus(rat.getRecordStatus());
                        rca.setVersion(clinvarAccession.getVersion().intValue());
                        rca.setCreated(new java.sql.Date(rat.getDateCreated().toGregorianCalendar().getTimeInMillis()));
                        rca.setUpdated(new java.sql.Date(clinvarAccession.getDateUpdated().toGregorianCalendar().getTimeInMillis()));

                        ClinicalSignificanceType clinicalSignificanceType = rat.getClinicalSignificance();
                        rca.setAssertionStatus(clinicalSignificanceType.getReviewStatus().value());
                        rca.setAssertion(clinicalSignificanceType.getDescription());
                        rca.setAssertionType(rat.getAssertion().getType().value());

                        TraitSetType traitSetType = rat.getTraitSet();

                        TraitSet cTraitSet = canvasDAOBeanService.getTraitSetDAO().findById(traitSetType.getID().intValue());
                        if (cTraitSet == null) {
                            cTraitSet = new TraitSet(traitSetType.getID().intValue(), traitSetType.getType());
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
                                    canvasDAOBeanService.getTraitSetDAO().save(cTraitSet);
                                    cTraitSet.getTraits().add(cTrait);
                                }
                            }

                        }

                        canvasDAOBeanService.getTraitSetDAO().save(cTraitSet);

                    } catch (Exception e) {
                        logger.error("Error", e);
                        e.printStackTrace();
                    }

                }

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private LocatedVariant parseSubstitution(GenomeRef genomeRef, GenomeRefSeq genomeRefSeq, List<VariantType> allVariantTypes,
            SubstitutionAlleleInfo alleleInfo) {
        LocatedVariant locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
        locatedVariant.setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("sub")).findAny().get());

        return locatedVariant;
    }

    private LocatedVariant parseInsertion(GenomeRef genomeRef, GenomeRefSeq genomeRefSeq, List<VariantType> allVariantTypes,
            InsertionAlleleInfo alleleInfo) {

        LocatedVariant locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
        locatedVariant.setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("ins")).findAny().get());

        return locatedVariant;
    }

    private LocatedVariant parseDuplication(GenomeRef genomeRef, GenomeRefSeq genomeRefSeq, List<VariantType> allVariantTypes,
            DuplicationAlleleInfo alleleInfo) {

        LocatedVariant locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
        locatedVariant.setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("ins")).findAny().get());

        String location = alleleInfo.getLocation();
        Pattern p = Pattern.compile("(?<start>\\d+)_(?<stop>\\d+)");
        Matcher m = p.matcher(location);
        if (m.find()) {
            locatedVariant.setPosition(Integer.valueOf(m.group("start")));
            locatedVariant.setEndPosition(Integer.valueOf(m.group("stop")));
        }

        if (locatedVariant.getPosition() == null && locatedVariant.getEndPosition() == null) {
            p = Pattern.compile("\\((?<firstStart>\\d+)_(?<firstStop>\\d+)\\)_\\((?<secondStart>\\d+)_(?<secondStop>\\d+)\\)");
            m = p.matcher(location);
            if (m.find()) {
                locatedVariant.setPosition(Integer.valueOf(m.group("start")));
                locatedVariant.setEndPosition(Integer.valueOf(m.group("stop")));
            }
        }

        // canvasDAOBeanService.getGenomeRefSeqLocationDAO().findByRefIdAndVersionedAccesionAndPosition(locatedVariant.getGenomeRef().getId(),
        // locatedVariant.getGenomeRefSeq().getVerAccession(), )
        // locatedVariant.setSeq();
        // locatedVariant.setRef(info.getBases());
        return locatedVariant;
    }

    private LocatedVariant parseDeletion(GenomeRef genomeRef, GenomeRefSeq genomeRefSeq, List<VariantType> allVariantTypes,
            DeletionAlleleInfo alleleInfo) {

        LocatedVariant locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);
        locatedVariant.setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("del")).findAny().get());

        String location = alleleInfo.getLocation();
        Pattern p = Pattern.compile("(?<start>\\d+)_(?<stop>\\d+)");
        Matcher m = p.matcher(location);
        if (m.find()) {
            locatedVariant.setPosition(Integer.valueOf(m.group("start")));
            locatedVariant.setEndPosition(Integer.valueOf(m.group("stop")));
        }

        if (locatedVariant.getPosition() == null && locatedVariant.getEndPosition() == null) {
            p = Pattern.compile("\\((?<firstStart>\\d+)_(?<firstStop>\\d+)\\)_\\((?<secondStart>\\d+)_(?<secondStop>\\d+)\\)");
            m = p.matcher(location);
            if (m.find()) {
                locatedVariant.setPosition(Integer.valueOf(m.group("start")));
                locatedVariant.setEndPosition(Integer.valueOf(m.group("stop")));
            }
        }

        locatedVariant.setSeq(alleleInfo.getBases());
        locatedVariant.setRef(alleleInfo.getBases());

        return locatedVariant;
    }

}
