package org.renci.canvas.primer.esp.commands;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

@Command(scope = "esp", name = "persist", description = "Persist ESP data")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private PrimerDAOBeanService annotationDAOBeanService;

    @Option(name = "--espFileName", description = "From http://evs.gs.washington.edu/EVS/", required = false, multiValued = false)
    private String espFileName = "ESP6500SI-V2-SSA137.GRCh38-liftover.snps_indels.txt.tar.gz";

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Path destinationParentPath = Paths.get(System.getProperty("karaf.data"), "tmp", "ESP");
        File destinationParentFile = destinationParentPath.toFile();

        VariantType snpVariantType = canvasDAOBeanService.getVariantTypeDAO().findById("snp");
        VariantType insVariantType = canvasDAOBeanService.getVariantTypeDAO().findById("ins");
        VariantType delVariantType = canvasDAOBeanService.getVariantTypeDAO().findById("del");

        GenomeRef genomeRef = canvasDAOBeanService.getGenomeRefDAO().findById(2);

        List<File> espFileList = Arrays.asList(destinationParentFile.listFiles());

        for (File entryFile : espFileList) {
            try (VCFFileReader vcfFileReader = new VCFFileReader(entryFile, false)) {

                VCFHeader vcfHeader = vcfFileReader.getFileHeader();
                List<String> sampleNames = vcfHeader.getGenotypeSamples();

                for (String sampleName : sampleNames) {

                    for (VariantContext variantContext : vcfFileReader) {

                        Allele refAllele = variantContext.getReference();

                        GenotypesContext genotypesContext = variantContext.getGenotypes();

                    }

                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return null;

    }

    public String getEspFileName() {
        return espFileName;
    }

    public void setEspFileName(String espFileName) {
        this.espFileName = espFileName;
    }

}
