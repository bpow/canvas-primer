package org.renci.canvas.primer.clinvar.commands;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.renci.canvas.dao.commons.LocatedVariantFactory;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.clinvar.MeasureSetType;
import org.renci.clinvar.MeasureTraitType;
import org.renci.clinvar.MeasureType;
import org.renci.clinvar.MeasureType.AttributeSet;
import org.renci.clinvar.PublicSetType;
import org.renci.clinvar.ReferenceAssertionType;
import org.renci.clinvar.ReleaseType;
import org.renci.clinvar.SequenceLocationType;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_38_7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scratch {

    private static final Logger logger = LoggerFactory.getLogger(Scratch.class);

    @Test
    public void hgvsLocation4Deletion() {

        String location = "123_456";
        Pattern p = Pattern.compile("(?<start>\\d+)_(?<stop>\\d+)");
        Matcher m = p.matcher(location);
        assertTrue(m.find());
        assertTrue(123 == Integer.valueOf(m.group("start")));
        assertTrue(456 == Integer.valueOf(m.group("stop")));

        location = "(12_34)_(56_78)";
        p = Pattern.compile("\\((?<firstStart>\\d+)_(?<firstStop>\\d+)\\)_\\((?<secondStart>\\d+)_(?<secondStop>\\d+)\\)");
        m = p.matcher(location);
        assertTrue(m.find());
        assertTrue(12 == Integer.valueOf(m.group("firstStart")));
        assertTrue(34 == Integer.valueOf(m.group("firstStop")));
        assertTrue(56 == Integer.valueOf(m.group("secondStart")));
        assertTrue(78 == Integer.valueOf(m.group("secondStop")));

    }

    @Test
    public void test() {

        List<String> schemaFileList = Arrays.asList("clinvar_public_1.1.xsd", "clinvar_public_1.10.xsd", "clinvar_public_1.11.xsd",
                "clinvar_public_1.12.xsd", "clinvar_public_1.14.xsd", "clinvar_public_1.15.xsd", "clinvar_public_1.16.xsd",
                "clinvar_public_1.17.xsd", "clinvar_public_1.18.xsd", "clinvar_public_1.19.xsd", "clinvar_public_1.2.xsd",
                "clinvar_public_1.20.xsd", "clinvar_public_1.21.xsd", "clinvar_public_1.22.xsd", "clinvar_public_1.24.xsd",
                "clinvar_public_1.27.xsd", "clinvar_public_1.29.xsd", "clinvar_public_1.3.xsd", "clinvar_public_1.33.xsd",
                "clinvar_public_1.34.xsd", "clinvar_public_1.35.xsd", "clinvar_public_1.36.xsd", "clinvar_public_1.37.xsd",
                "clinvar_public_1.38.xsd", "clinvar_public_1.39.xsd", "clinvar_public_1.4.xsd", "clinvar_public_1.41.xsd",
                "clinvar_public_1.42.xsd", "clinvar_public_1.5.xsd", "clinvar_public_1.6.xsd", "clinvar_public_1.7.xsd",
                "clinvar_public_1.8.xsd", "clinvar_public_1.9.xsd");

        List<Pair<Integer, Integer>> versionPairList = new ArrayList<>();
        Pattern p = Pattern.compile("clinvar_public_(?<v1>\\d+)\\.(?<v2>\\d+)\\.xsd");

        for (String schemaFile : schemaFileList) {
            Matcher m = p.matcher(schemaFile);
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
        System.out.println(versionPairList.get(0).getLeft());
        System.out.println(versionPairList.get(0).getRight());
    }

    @Test
    public void scratchHGVS() {
        File output = new File("/tmp", "asdf.txt");

        File clinvarXmlFile = new File("/tmp", "ClinVarFullRelease_2017-03.xml.gz");
        try (FileWriter fw = new FileWriter(output);
                BufferedWriter bw = new BufferedWriter(fw);
                FileInputStream fis = new FileInputStream(clinvarXmlFile);
                GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue())) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(gzis);

            JAXBContext jc = JAXBContext.newInstance(ReleaseType.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<ReleaseType> releaseType = u.unmarshal(reader, ReleaseType.class);
            List<PublicSetType> publicSetTypeList = releaseType.getValue().getClinVarSet();

            for (PublicSetType pst : publicSetTypeList) {
                ReferenceAssertionType rat = pst.getReferenceClinVarAssertion();
                MeasureSetType measureSetType = rat.getMeasureSet();
                List<MeasureType> measureTypes = measureSetType.getMeasure();
                if (CollectionUtils.isNotEmpty(measureTypes)) {
                    for (MeasureType measureType : measureTypes) {
                        List<AttributeSet> attributeSet = measureType.getAttributeSet().stream()
                                .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level")
                                        && a.getAttribute().getValue().startsWith("NC_") && a.getAttribute().getIntegerValue() != 36
                                        && !a.getAttribute().getValue().contains("?"))
                                .collect(Collectors.toList());
                        if (CollectionUtils.isNotEmpty(attributeSet)) {
                            for (AttributeSet a : attributeSet) {
                                bw.write(String.format("integerValue = %s, value = %s", a.getAttribute().getIntegerValue(),
                                        a.getAttribute().getValue()));
                                bw.newLine();
                                bw.flush();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void logAmbiguousHGVS() {
        File output = new File("/tmp", "qwer.txt");

        File clinvarXmlFile = new File("/home/jdr0887/Downloads", "ClinVarFullRelease_00-latest.xml.gz");
        try (FileWriter fw = new FileWriter(output);
                BufferedWriter bw = new BufferedWriter(fw);
                FileInputStream fis = new FileInputStream(clinvarXmlFile);
                GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue())) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(gzis);

            JAXBContext jc = JAXBContext.newInstance(ReleaseType.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<ReleaseType> releaseType = u.unmarshal(reader, ReleaseType.class);
            List<PublicSetType> publicSetTypeList = releaseType.getValue().getClinVarSet();

            for (PublicSetType pst : publicSetTypeList) {
                ReferenceAssertionType rat = pst.getReferenceClinVarAssertion();
                MeasureSetType measureSetType = rat.getMeasureSet();
                List<MeasureType> measures = measureSetType.getMeasure();
                if (CollectionUtils.isNotEmpty(measures)) {
                    for (MeasureType measure : measures) {
                        List<AttributeSet> attributeSet = measure.getAttributeSet().stream()
                                .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level")
                                        && StringUtils.isNotEmpty(a.getAttribute().getValue())
                                        && a.getAttribute().getValue().startsWith("NC_") && a.getAttribute().getIntegerValue() != 36
                                        && a.getAttribute().getValue().contains("?"))
                                .collect(Collectors.toList());
                        if (CollectionUtils.isNotEmpty(attributeSet)) {
                            for (AttributeSet a : attributeSet) {
                                bw.write(String.format("integerValue = %s, value = %s", a.getAttribute().getIntegerValue(),
                                        a.getAttribute().getValue()));
                                bw.newLine();
                                bw.flush();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void printMeasureTypeCounts() {
        File clinvarXmlFile = new File("/home/jdr0887/Downloads", "ClinVarFullRelease_00-latest.xml.gz");
        try (FileInputStream fis = new FileInputStream(clinvarXmlFile);
                GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue())) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(gzis);

            JAXBContext jc = JAXBContext.newInstance(ReleaseType.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<ReleaseType> releaseType = u.unmarshal(reader, ReleaseType.class);
            List<PublicSetType> publicSetTypeList = releaseType.getValue().getClinVarSet();

            Map<String, Integer> measureTypeCountMap = new HashMap<>();

            for (PublicSetType pst : publicSetTypeList) {
                ReferenceAssertionType rat = pst.getReferenceClinVarAssertion();
                MeasureSetType measureSetType = rat.getMeasureSet();
                List<MeasureType> measures = measureSetType.getMeasure();
                if (CollectionUtils.isNotEmpty(measures)) {
                    for (MeasureType measure : measures) {

                        String measureType = measure.getType();

                        if (!measureTypeCountMap.containsKey(measureType)) {
                            measureTypeCountMap.put(measureType, 1);
                            continue;
                        }

                        Integer count = measureTypeCountMap.get(measureType);
                        measureTypeCountMap.put(measureType, count + 1);

                    }
                }
            }

            for (String key : measureTypeCountMap.keySet()) {
                System.out.println(String.format("%s: %s", key, measureTypeCountMap.get(key)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Duplication: 3841
        // Insertion: 1454
        // fusion: 8
        // single nucleotide variant: 137783
        // protein only: 79
        // inversion: 3
        // Deletion: 14008
        // copy number gain: 7303
        // copy number loss: 6856
        // Variation: 496
        // Indel: 1047

    }

    @Test
    public void useSequenceLocations() {
        File clinvarXmlFile = new File("/home/jdr0887/Downloads", "ClinVarFullRelease_00-latest.xml.gz");
        GenomeRef genomeRef = null;
        GenomeRefSeq genomeRefSeq = null;

        List<String> measureTypeExcludes = Arrays.asList("Indel", "Microsatellite", "Inversion", "Variation");

        List<VariantType> allVariantTypes = Arrays.asList(new VariantType("snp"), new VariantType("sub"), new VariantType("ins"),
                new VariantType("del"));

        List<String> allowableAccessions = Arrays.asList("RCV000485317", "RCV000244801", "RCV000207071", "RCV000244801", "RCV000480151",
                "RCV000250462");

        try (FileInputStream fis = new FileInputStream(clinvarXmlFile);
                GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue());
                FileWriter fw = new FileWriter(new File("/tmp", "ClinVar-LocatedVariant.txt"));
                BufferedWriter bw = new BufferedWriter(fw)) {

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

                    if (!allowableAccessions.contains(rat.getClinVarAccession().getAcc())) {
                        continue;
                    }

                    bw.write(rat.getClinVarAccession().getAcc());
                    bw.newLine();
                    bw.flush();

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

                            if (CollectionUtils.isNotEmpty(filters) && CollectionUtils.isNotEmpty(
                                    filters.stream().filter(a -> a.getAttribute().getValue().contains("?")).collect(Collectors.toList()))) {
                                continue;
                            }

                            if (measureTypeExcludes.contains(measure.getType())) {
                                continue;
                            }

                            for (SequenceLocationType sequenceLocationType : measure.getSequenceLocation()) {

                                if (!sequenceLocationType.isSetPositionVCF() || !sequenceLocationType.isSetReferenceAlleleVCF()
                                        || !sequenceLocationType.isSetAlternateAlleleVCF()) {
                                    continue;
                                }

                                String alt = StringUtils.isNotEmpty(sequenceLocationType.getAlternateAlleleVCF())
                                        && !sequenceLocationType.getAlternateAlleleVCF().equals("-")
                                                ? sequenceLocationType.getAlternateAlleleVCF() : "";

                                Range<Integer> range = Range.between(sequenceLocationType.getPositionVCF().intValue(),
                                        sequenceLocationType.getPositionVCF().intValue()
                                                + sequenceLocationType.getReferenceAlleleVCF().length());

                                if ("GRCh38".equals(sequenceLocationType.getAssembly())) {

                                    String refBase = GeReSe4jBuild_38_7.getInstance(new File("/home/jdr0887/gerese4j"))
                                            .getRegion(sequenceLocationType.getAccession(), range, true);

                                    LocatedVariant locatedVariant = LocatedVariantFactory.create(genomeRef, genomeRefSeq,
                                            sequenceLocationType.getPositionVCF().intValue(), refBase, alt, allVariantTypes);

                                    if (locatedVariant != null) {
                                        bw.write(locatedVariant.toString());
                                        bw.newLine();
                                        bw.flush();
                                    }

                                }

                                if ("GRCh37".equals(sequenceLocationType.getAssembly())) {

                                    String refBase = GeReSe4jBuild_37_3.getInstance(new File("/home/jdr0887/gerese4j"))
                                            .getRegion(sequenceLocationType.getAccession(), range, true);

                                    LocatedVariant locatedVariant = LocatedVariantFactory.create(genomeRef, genomeRefSeq,
                                            sequenceLocationType.getPositionVCF().intValue(), refBase, alt, allVariantTypes);

                                    if (locatedVariant != null) {
                                        bw.write(locatedVariant.toString());
                                        bw.newLine();
                                        bw.flush();
                                    }

                                }

                            }

                        }
                    }

                } else {
                    reader.next();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void countSequenceLocationsWithGoodAlleleVCF() {
        File clinvarXmlFile = new File("/home/jdr0887/Downloads", "ClinVarFullRelease_00-latest.xml.gz");
        int count = 0;
        try (FileInputStream fis = new FileInputStream(clinvarXmlFile);
                GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 14)).intValue())) {

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

                        for (MeasureType measure : measures) {

                            List<AttributeSet> filters = measure.getAttributeSet().stream()
                                    .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level"))
                                    .collect(Collectors.toList());

                            if (CollectionUtils.isEmpty(filters)) {
                                continue;
                            }

                            if (CollectionUtils.isNotEmpty(filters) && CollectionUtils.isNotEmpty(
                                    filters.stream().filter(a -> a.getAttribute().getValue().contains("?")).collect(Collectors.toList()))) {
                                continue;
                            }

                            for (SequenceLocationType sequenceLocationType : measure.getSequenceLocation()) {

                                if (sequenceLocationType.getVariantLength() != null
                                        && sequenceLocationType.getVariantLength().intValue() > 100) {
                                    continue;
                                }

                                if (!sequenceLocationType.isSetPositionVCF() || !sequenceLocationType.isSetReferenceAlleleVCF()
                                        || !sequenceLocationType.isSetAlternateAlleleVCF()) {
                                    continue;
                                }

                                count++;

                            }

                        }
                    }

                } else {
                    reader.next();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(count);

    }

    @Test
    public void download() {
        FTPFactory.ncbiDownload(new File("/tmp"), "/pub/clinvar/xml", "ClinVarFullRelease_00-latest.xml.gz");
    }

    @Test
    public void scratch() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File("/home/jdr0887/Downloads", "ClinVarFullRelease_00-latest.xml.gz"));
                GZIPInputStream gzis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue())) {

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(gzis);

            JAXBContext jc = JAXBContext.newInstance(ReleaseType.class, ReferenceAssertionType.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            QName clinvarSetQName = new QName("ClinVarSet");

            List<PublicSetType> pstList = new ArrayList<>();

            XMLEvent xmlEvent = null;
            while ((xmlEvent = reader.peek()) != null) {

                if (xmlEvent.isStartElement() && ((StartElement) xmlEvent).getName().equals(clinvarSetQName)) {

                    PublicSetType pst = unmarshaller.unmarshal(reader, PublicSetType.class).getValue();
                    ReferenceAssertionType rat = pst.getReferenceClinVarAssertion();

                    List<MeasureTraitType> measureTraitTypeList = pst.getClinVarAssertion();

                    for (MeasureTraitType mtt : measureTraitTypeList) {
                        if (mtt.getClinicalSignificance().getDescription().size() > 1) {
                            System.out.println("asdfadsfasdf");
                        }
                    }

                    MeasureSetType measureSetType = rat.getMeasureSet();

                    if (measureSetType != null && "Variant".equals(measureSetType.getType())) {

                        List<MeasureType> measures = measureSetType.getMeasure();

                        if (CollectionUtils.isEmpty(measures)) {
                            continue;
                        }

                        for (MeasureType measureType : measures) {

                            List<AttributeSet> filters = measureType.getAttributeSet().stream()
                                    .filter(a -> a.getAttribute().getType().startsWith("HGVS, genomic, top level"))
                                    .collect(Collectors.toList());

                            if (CollectionUtils.isEmpty(filters)
                                    || (CollectionUtils.isNotEmpty(filters) && CollectionUtils.isNotEmpty(filters.stream()
                                            .filter(a -> a.getAttribute().getValue().contains("?")).collect(Collectors.toList())))) {
                                continue;
                            }

                            List<SequenceLocationType> sequenceLocationTypeList = measureType.getSequenceLocation();

                            boolean okToAdd = false;
                            for (SequenceLocationType sequenceLocationType : sequenceLocationTypeList) {
                                if (sequenceLocationType.getStart() != null && (sequenceLocationType.getVariantLength() != null
                                        && sequenceLocationType.getVariantLength().intValue() < 100
                                        && StringUtils.isNotEmpty(sequenceLocationType.getAlternateAllele()))) {
                                    okToAdd = true;
                                }
                            }

                            if (okToAdd) {
                                pstList.add(pst);
                            }

                        }

                    }
                } else {
                    reader.next();
                }
            }
            System.out.println(pstList.size());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
