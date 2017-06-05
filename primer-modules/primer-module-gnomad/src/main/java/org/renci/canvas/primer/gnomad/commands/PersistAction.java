package org.renci.canvas.primer.gnomad.commands;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.gnomad.model.GnomADVariantFrequency;
import org.renci.canvas.dao.gnomad.model.GnomADVariantFrequencyPK;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

@Command(scope = "gnomad", name = "persist", description = "")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Option(name = "--gnomadExomesVCF", required = true, multiValued = false)
    private String gnomadExomesVCF;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Pair<String, File> versionAndFilePair = downloadLatest();

        String version = versionAndFilePair.getLeft();
        File latestFile = versionAndFilePair.getRight();

        List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();
        GenomeRef genomeRef = allGenomeRefs.stream().filter(a -> a.getName().equals("38.2")).findFirst().get();

        List<VariantType> allVariantTypes = canvasDAOBeanService.getVariantTypeDAO().findAll();

        List<GenomeRefSeq> allGenomeRefSeqs = canvasDAOBeanService.getGenomeRefSeqDAO().findByGenomeRefIdAndSeqType(genomeRef.getId(),
                "Chromosome");

        try (VCFFileReader vcfFileReader = new VCFFileReader(latestFile, false)) {

            for (VariantContext variantContext : vcfFileReader) {

                GenomeRefSeq genomeRefSeq = allGenomeRefSeqs.stream().filter(a -> a.getContig().equals(variantContext.getContig()))
                        .findFirst().get();

                CommonInfo commonInfo = variantContext.getCommonInfo();

                for (Allele altAllele : variantContext.getAlternateAlleles()) {

                    String ref = variantContext.getReference().getDisplayString();
                    String alt = altAllele.getDisplayString();

                    char[] referenceChars = ref.toCharArray();
                    char[] alternateChars = alt.toCharArray();

                    LocatedVariant locatedVariant = new LocatedVariant(genomeRef, genomeRefSeq);

                    if (variantContext.isSNP()) {

                        locatedVariant.setSeq(alt);
                        locatedVariant.setRef(ref);
                        locatedVariant.setPosition(variantContext.getStart());
                        locatedVariant.setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("snp")).findAny().get());
                        locatedVariant.setEndPosition(variantContext.getStart() + locatedVariant.getRef().length());

                    } else if (variantContext.isIndel()) {

                        // could be insertion or deletion

                        if (StringUtils.isNotEmpty(ref) && StringUtils.isNotEmpty(alt)) {

                            if (ref.length() > alt.length()) {

                                locatedVariant
                                        .setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("del")).findAny().get());
                                locatedVariant.setPosition(variantContext.getStart());

                                if (referenceChars.length > 1 && alternateChars.length > 1) {

                                    StringBuilder frontChars2Remove = new StringBuilder();

                                    for (int i = 0; i < referenceChars.length; ++i) {
                                        if (i == alternateChars.length || referenceChars[i] != alternateChars[i]) {
                                            break;
                                        }
                                        frontChars2Remove.append(referenceChars[i]);
                                    }

                                    if (frontChars2Remove.length() > 0) {
                                        ref = ref.replaceFirst(frontChars2Remove.toString(), "");
                                    }

                                    locatedVariant.setPosition(
                                            variantContext.getStart() + (frontChars2Remove.length() > 0 ? frontChars2Remove.length() : 0));
                                }
                                locatedVariant.setEndPosition(locatedVariant.getPosition() + ref.length());
                                locatedVariant.setRef(ref);
                                locatedVariant.setSeq(ref);

                            } else {

                                locatedVariant
                                        .setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("ins")).findAny().get());
                                locatedVariant.setPosition(variantContext.getStart() - 1);

                                if (referenceChars.length > 1 && alternateChars.length > 1) {

                                    StringBuilder frontChars2Remove = new StringBuilder();
                                    StringBuilder backChars2Remove = new StringBuilder();

                                    for (int i = 0; i < referenceChars.length; ++i) {
                                        if (referenceChars[i] != alternateChars[i]) {
                                            break;
                                        }
                                        frontChars2Remove.append(referenceChars[i]);
                                    }

                                    for (int i = referenceChars.length - 1; i > 0; --i) {
                                        if (referenceChars[i] != alternateChars[i]) {
                                            break;
                                        }
                                        backChars2Remove.append(referenceChars[i]);
                                    }

                                    if (frontChars2Remove.length() > 0) {
                                        ref = ref.replaceFirst(frontChars2Remove.toString(), "");
                                        alt = alt.replaceFirst(frontChars2Remove.toString(), "");
                                    }

                                    if (backChars2Remove.length() > 0) {
                                        backChars2Remove.reverse();
                                        ref = StringUtils.removeEnd(ref, backChars2Remove.toString());
                                        alt = StringUtils.removeEnd(alt, backChars2Remove.toString());
                                    }

                                    locatedVariant.setPosition(variantContext.getStart() - 1
                                            + (frontChars2Remove.length() > 0 ? frontChars2Remove.length() : 0));
                                }

                                locatedVariant.setEndPosition(locatedVariant.getPosition() + 1);
                                locatedVariant.setRef("");
                                locatedVariant.setSeq(alt);
                            }

                        }

                    } else if (variantContext.isMNP()) {

                        locatedVariant.setVariantType(allVariantTypes.stream().filter(a -> a.getId().equals("sub")).findAny().get());
                        locatedVariant.setPosition(variantContext.getStart());
                        locatedVariant.setRef(ref);
                        locatedVariant.setSeq(alt);
                        locatedVariant.setEndPosition(locatedVariant.getPosition() + ref.length());

                    }

                    List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findByExample(locatedVariant);
                    if (CollectionUtils.isNotEmpty(foundLocatedVariants)) {
                        locatedVariant = foundLocatedVariants.get(0);
                    } else {
                        locatedVariant.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
                    }
                    logger.debug(locatedVariant.toString());

                    for (String population : Arrays.asList("AFR", "AMR", "ASJ", "EAS", "FIN", "NFE", "OTH", "SAS")) {

                        Double alleleFrequency = commonInfo.getAttributeAsDouble(String.format("AF_%s", population), 0D);
                        Integer alleleCount = commonInfo.getAttributeAsInt(String.format("AC_%s", population), 0);
                        Integer alleleTotal = commonInfo.getAttributeAsInt(String.format("AN_%s", population), 0);
                        Integer hemizygousCount = commonInfo.getAttributeAsInt(String.format("Hemi_%s", population), 0);
                        Integer homozygousCount = commonInfo.getAttributeAsInt(String.format("Hom_%s", population), 0);

                        GnomADVariantFrequencyPK variantFrequencyPK = new GnomADVariantFrequencyPK(locatedVariant.getId(), version,
                                population);
                        GnomADVariantFrequency variantFrequency = new GnomADVariantFrequency(variantFrequencyPK);

                        variantFrequency.setAlternateAlleleFrequency(alleleFrequency);
                        variantFrequency.setAlternateAlleleCount(alleleCount);
                        variantFrequency.setTotalAlleleCount(alleleTotal);
                        variantFrequency.setHemizygousCount(hemizygousCount);
                        variantFrequency.setHomozygousCount(homozygousCount);

                    }

                }

            }
        }
        return null;
    }

    private Pair<String, File> downloadLatest() {

        File download = new File("/home/jdr0887/Downloads", "gnomad.exomes.r2.0.1.sites.vcf");

        Pattern p = Pattern.compile("gnomad\\.exomes\\.r(?<version>\\d\\.\\d\\.\\d)\\.sites\\.vcf");
        Matcher m = p.matcher(download.getName());
        m.find();
        String version = null;
        if (m.matches()) {
            version = m.group("version");
        }

        return Pair.of(version, download);
    }

    public String getGnomadExomesVCF() {
        return gnomadExomesVCF;
    }

    public void setGnomadExomesVCF(String gnomadExomesVCF) {
        this.gnomadExomesVCF = gnomadExomesVCF;
    }

}
