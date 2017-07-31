package org.renci.canvas.primer.thousandgenomes;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class Scratch {

    private static final Logger logger = LoggerFactory.getLogger(Scratch.class);

    public Scratch() {
        super();
    }

    @Test
    public void scratch() {

        File vcf = new File("/home/jdr0887/Downloads", "ALL.chr22.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz");
        try (VCFFileReader vcfFileReader = new VCFFileReader(vcf, false)) {

            for (VariantContext variantContext : vcfFileReader) {

                CommonInfo commonInfo = variantContext.getCommonInfo();

                for (Allele altAllele : variantContext.getAlternateAlleles()) {

                    // LocatedVariant locatedVariant = LocatedVariantFactory.create(genomeRef, genomeRefSeq, variantContext, altAllele,
                    // allVariantTypes);
                    // logger.info(locatedVariant.toString());

                    int alleleIndex = variantContext.getAlleleIndex(altAllele);
                    logger.debug("alleleIndex: {}", alleleIndex);

                    Arrays.asList("AFR", "AMR", "EAS", "EUR", "SAS").parallelStream().forEach(population -> {

                        try {

                            String alleleFrequencyValue = commonInfo.getAttributeAsString(String.format("%s_AF", population), "");
                            String alleleCountValue = commonInfo.getAttributeAsString("AC", "");
                            String alleleTotalValue = commonInfo.getAttributeAsString("AN", "");

                            Integer alleleTotal = StringUtils.isNotEmpty(alleleTotalValue) && !".".equals(alleleTotalValue)
                                    ? Integer.valueOf(alleleTotalValue) : 0;

                            Integer alleleCount = StringUtils.isNotEmpty(alleleTotalValue) && !".".equals(alleleTotalValue)
                                    ? Integer.valueOf(alleleTotalValue) : 0;

                            Double alleleFrequency = null;

                            if (alleleIndex > 0) {

                                List<String> alleleFrequencyValues = commonInfo.getAttributeAsStringList(String.format("%s_AF", population),
                                        "");
                                if (CollectionUtils.isNotEmpty(alleleFrequencyValues)) {
                                    alleleFrequencyValue = alleleFrequencyValues.get(alleleIndex - 1);
                                    alleleFrequency = StringUtils.isNotEmpty(alleleFrequencyValue) && !".".equals(alleleFrequencyValue)
                                            ? Double.valueOf(alleleFrequencyValue) : 0D;
                                } else {
                                    alleleFrequency = 0D;
                                }

                            } else {

                                alleleFrequency = StringUtils.isNotEmpty(alleleFrequencyValue) && !".".equals(alleleFrequencyValue)
                                        ? Double.valueOf(alleleFrequencyValue) : 0D;
                                alleleCount = StringUtils.isNotEmpty(alleleCountValue) && !".".equals(alleleCountValue)
                                        ? Integer.valueOf(alleleCountValue) : 0;
                                alleleTotal = StringUtils.isNotEmpty(alleleTotalValue) && !".".equals(alleleTotalValue)
                                        ? Integer.valueOf(alleleTotalValue) : 0;

                            }

                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }

                    });

                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
