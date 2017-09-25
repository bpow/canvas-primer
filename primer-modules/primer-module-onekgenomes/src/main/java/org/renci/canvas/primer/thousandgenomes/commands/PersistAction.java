package org.renci.canvas.primer.thousandgenomes.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.commons.LocatedVariantFactory;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

@Command(scope = "1000genomes", name = "persist", description = "")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().submit(() -> {

            try {

                List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();
                GenomeRef genomeRef = allGenomeRefs.stream().filter(a -> a.getName().startsWith("37"))
                        .sorted((a, b) -> a.getName().compareTo(b.getName())).findFirst().get();

                List<VariantType> allVariantTypes = canvasDAOBeanService.getVariantTypeDAO().findAll();

                List<GenomeRefSeq> allGenomeRefSeqs = canvasDAOBeanService.getGenomeRefSeqDAO()
                        .findByGenomeRefIdAndSeqType(genomeRef.getId(), "Chromosome");

                Path outputPath = Paths.get(System.getProperty("karaf.data"), "1000genomes");
                File outputDir = outputPath.toFile();
                outputDir.mkdirs();

                List<File> vcfs = FTPFactory.ncbiDownloadFiles(outputDir, "/1000genomes/ftp/release/20130502", "ALL.chr", "vcf.gz");

                List<File> files = new ArrayList<>();

                for (File vcf : vcfs) {

                    try (VCFFileReader vcfFileReader = new VCFFileReader(vcf, false)) {

                        for (VariantContext variantContext : vcfFileReader) {

                            GenomeRefSeq genomeRefSeq = allGenomeRefSeqs.stream()
                                    .filter(a -> a.getContig().equals(variantContext.getContig())).findFirst().get();

                            CommonInfo commonInfo = variantContext.getCommonInfo();

                            for (Allele altAllele : variantContext.getAlternateAlleles()) {

                                LocatedVariant locatedVariant = LocatedVariantFactory.create(genomeRef, genomeRefSeq,
                                        variantContext.getStart(), variantContext.getReference().getDisplayString(),
                                        altAllele.getDisplayString(), allVariantTypes);

                                logger.info(locatedVariant.toString());

                                int alleleIndex = variantContext.getAlleleIndex(altAllele);
                                logger.debug("alleleIndex: {}", alleleIndex);

                                LocatedVariantWithAttributesWrapper lvWrapper = new LocatedVariantWithAttributesWrapper(locatedVariant);

                                Arrays.asList("AFR", "AMR", "EAS", "EUR", "SAS").parallelStream().forEach(population -> {

                                    try {

                                        String alleleFrequencyValue = commonInfo.getAttributeAsString(String.format("%s_AF", population),
                                                "");
                                        String alleleCountValue = commonInfo.getAttributeAsString("AC", "");
                                        String alleleTotalValue = commonInfo.getAttributeAsString("AN", "");

                                        Double alleleTotal = StringUtils.isNotEmpty(alleleTotalValue) && !".".equals(alleleTotalValue)
                                                ? Double.valueOf(alleleTotalValue) : 0D;

                                        Double alleleCount = StringUtils.isNotEmpty(alleleCountValue) && !".".equals(alleleCountValue)
                                                ? Double.valueOf(alleleCountValue) : 0D;

                                        Double alleleFrequency = null;

                                        if (alleleIndex > 0) {

                                            List<String> alleleFrequencyValues = commonInfo
                                                    .getAttributeAsStringList(String.format("AF_%s", population), "");
                                            if (CollectionUtils.isNotEmpty(alleleFrequencyValues)) {
                                                alleleFrequencyValue = alleleFrequencyValues.get(alleleIndex - 1);
                                                alleleFrequency = StringUtils.isNotEmpty(alleleFrequencyValue)
                                                        && !".".equals(alleleFrequencyValue) ? Double.valueOf(alleleFrequencyValue) : 0D;
                                            } else {
                                                alleleFrequency = 0D;
                                            }

                                        } else {

                                            alleleFrequency = StringUtils.isNotEmpty(alleleFrequencyValue)
                                                    && !".".equals(alleleFrequencyValue) ? Double.valueOf(alleleFrequencyValue) : 0D;

                                        }

                                        lvWrapper.getAttributes().put(String.format("%s_AF", population), alleleFrequency);
                                        lvWrapper.getAttributes().put(String.format("%s_AN", population), alleleTotal);
                                        lvWrapper.getAttributes().put(String.format("%s_AC", population), alleleCount);

                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                    }

                                });

                                File dir = new File(outputDir, variantContext.getContig());
                                dir.mkdirs();

                                File file = new File(dir, String.format("%s.txt", UUID.randomUUID().toString()));
                                files.add(file);

                                try (FileOutputStream fos = new FileOutputStream(file);
                                        GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                                        ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                                    oos.writeObject(lvWrapper);
                                }

                            }

                        }
                    }

                }

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        });

        return null;
    }

    private class LocatedVariantWithAttributesWrapper implements Serializable {

        private static final long serialVersionUID = 1066600018992442816L;

        private LocatedVariant locatedVariant;

        private Map<String, Double> attributes;

        public LocatedVariantWithAttributesWrapper(LocatedVariant locatedVariant) {
            super();
            this.locatedVariant = locatedVariant;
            this.attributes = new HashMap<>();
        }

        public LocatedVariant getLocatedVariant() {
            return locatedVariant;
        }

        public void setLocatedVariant(LocatedVariant locatedVariant) {
            this.locatedVariant = locatedVariant;
        }

        public Map<String, Double> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Double> attributes) {
            this.attributes = attributes;
        }

    }

}
