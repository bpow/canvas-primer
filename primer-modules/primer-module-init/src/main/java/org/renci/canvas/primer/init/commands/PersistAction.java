package org.renci.canvas.primer.init.commands;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinbin.model.DiagnosticStatusType;
import org.renci.canvas.dao.clinbin.model.IncidentalStatusType;
import org.renci.canvas.dao.clinbin.model.MaxFrequencySource;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.SequenceType;
import org.renci.canvas.dao.refseq.model.LocationType;
import org.renci.canvas.dao.var.model.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "init", name = "persist", description = "Persist Initialization data (dictionary stuff)")
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

        // these should all become Java enums
        logger.info("Loading VariantTypes");
        List<String> vtList = Arrays.asList("snp", "ins", "del", "sub", "ref");
        List<VariantType> variantTypeList = canvasDAOBeanService.getVariantTypeDAO().findAll();
        for (String vt : vtList) {
            if (!variantTypeList.stream().filter(a -> a.getName().equals(vt)).findAny().isPresent()) {
                VariantType variantType = new VariantType(vt);
                canvasDAOBeanService.getVariantTypeDAO().save(variantType);
            }
        }

        logger.info("Loading LocationTypes");
        List<String> ltList = Arrays.asList("UTR", "UTR-5", "UTR-3", "intron", "exon", "intergenic", "potential RNA-editing site",
                "intron/exon boundary");
        List<LocationType> locationTypeList = canvasDAOBeanService.getLocationTypeDAO().findAll();
        for (String lt : ltList) {
            if (!locationTypeList.stream().filter(a -> a.getName().equals(lt)).findAny().isPresent()) {
                LocationType locationType = new LocationType(lt);
                canvasDAOBeanService.getLocationTypeDAO().save(locationType);
            }
        }

        logger.info("Loading SequenceTypes");
        List<String> stList = Arrays.asList("Alternate Loci", "Chromosome", "Fix Patch", "Mitochondrial Genome", "Novel Patch",
                "Unlocalized Contig", "Unplaced Contig");
        List<SequenceType> seqTypeList = canvasDAOBeanService.getSequenceTypeDAO().findAll();
        for (String st : stList) {
            if (!seqTypeList.stream().filter(a -> a.getName().equals(st)).findAny().isPresent()) {
                SequenceType sequenceType = new SequenceType(st);
                canvasDAOBeanService.getSequenceTypeDAO().save(sequenceType);
            }
        }

        logger.info("Loading DiagnosticStatusTypes");
        List<String> dstList = Arrays.asList("VCF loading", "Annotating variants", "Updating frequency table", "Updating dx bins",
                "Calculating dx bin results", "Complete", "Failed", "Requested", "VCF loaded", "Annotated variants",
                "Updated frequency table", "Updated dx bins", "Calculated dx bin results", "Updating rs_ids", "Updated rs_ids",
                "Generating Report", "Paused");
        List<DiagnosticStatusType> diagnosticStatusTypeList = canvasDAOBeanService.getDiagnosticStatusTypeDAO().findAll();
        for (String dst : dstList) {
            if (!diagnosticStatusTypeList.stream().filter(a -> a.getName().equals(dst)).findAny().isPresent()) {
                DiagnosticStatusType diagnosticStatusType = new DiagnosticStatusType(dst);
                canvasDAOBeanService.getDiagnosticStatusTypeDAO().save(diagnosticStatusType);
            }
        }

        logger.info("Loading IncidentalStatusTypes");
        List<String> istList = Arrays.asList("VCF loading", "Annotating variants", "Updating frequency table", "Updating incidental bins",
                "Calculating incidental bin results", "Complete", "Failed", "Requested", "VCF loaded", "Annotated variants",
                "Updated frequency table", "Updated incidental bins", "Calculated incidental bin results", "Updating rs_ids",
                "Updated rs_ids", "Loading Missing", "Loaded Missing", "Paused");
        List<IncidentalStatusType> incidentalStatusTypeList = canvasDAOBeanService.getIncidentalStatusTypeDAO().findAll();
        for (String ist : istList) {
            if (!incidentalStatusTypeList.stream().filter(a -> a.getName().equals(ist)).findAny().isPresent()) {
                IncidentalStatusType incidentalStatusType = new IncidentalStatusType(ist);
                canvasDAOBeanService.getIncidentalStatusTypeDAO().save(incidentalStatusType);
            }
        }

        logger.info("Loading MaxFrequencySource");
        List<String> mfsList = Arrays.asList("snp", "indel", "none");
        List<MaxFrequencySource> maxFrequencySourceList = canvasDAOBeanService.getMaxFrequencySourceDAO().findAll();
        for (String mfs : mfsList) {
            if (!maxFrequencySourceList.stream().filter(a -> a.getName().equals(mfs)).findAny().isPresent()) {
                MaxFrequencySource maxFrequencySource = new MaxFrequencySource(mfs);
                canvasDAOBeanService.getMaxFrequencySourceDAO().save(maxFrequencySource);
            }
        }

        logger.info("Loading GenomeRef");
        List<GenomeRef> genomeRefList = canvasDAOBeanService.getGenomeRefDAO().findAll();
        List<Pair<Integer, String>> versions = Arrays.asList(Pair.of(1, "36.1"), Pair.of(2, "37.1"), Pair.of(3, "37.2"),
                Pair.of(4, "38.2"));
        for (Pair<Integer, String> version : versions) {
            if (!genomeRefList.stream().filter(a -> a.getId().equals(version.getLeft()) && a.getName().equals(version.getRight())).findAny()
                    .isPresent()) {
                GenomeRef genomeRef = new GenomeRef("NCBI", String.format("BUILD.%s", version.getRight()), version.getRight());
                genomeRef.setId(version.getLeft());
                canvasDAOBeanService.getGenomeRefDAO().save(genomeRef);
            }
        }

        return null;
    }

}
