package org.renci.canvas.primer.gr.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.ReferenceSequence;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_38_7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

public class CreateCombinedFastaFileCallable implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CreateCombinedFastaFileCallable.class);

    private final List<String> nonExtraSequenceTypes = Arrays.asList("Chromosome", "Unlocalized Contig", "Unplaced Contig",
            "Mitochondrial Genome");

    private File outputDirectory;

    private CANVASDAOBeanService canvasDAOBeanService;

    public CreateCombinedFastaFileCallable(File outputDirectory, CANVASDAOBeanService canvasDAOBeanService) {
        super();
        this.outputDirectory = outputDirectory;
        this.canvasDAOBeanService = canvasDAOBeanService;
    }

    @Override
    public Void call() throws Exception {
        logger.debug("ENTERING call()");

        long start = System.currentTimeMillis();

        try {

            List<GeReSe4jBuild> gerese4jBuilds = Arrays.asList(GeReSe4jBuild_37_3.getInstance(), GeReSe4jBuild_38_7.getInstance());

            List<GenomeRef> genomeRefList = canvasDAOBeanService.getGenomeRefDAO().findAll();

            for (GeReSe4jBuild gerese4jBuild : gerese4jBuilds) {

                Set<String> indexSet = gerese4jBuild.getIndices();
                Optional<GenomeRef> optionalGenomeRef = genomeRefList.stream()
                        .filter(a -> a.getName().equals(gerese4jBuild.getBuild().getVersion())).findAny();
                if (optionalGenomeRef.isPresent()) {
                    GenomeRef genomeRef = optionalGenomeRef.get();
                    logger.info(genomeRef.toString());

                    File combinedFastaFile = new File(outputDirectory, String.format("%s.fa", genomeRef.getRefVer()));
                    logger.info(combinedFastaFile.getAbsolutePath());
                    File combinedFastaExtrasFile = new File(outputDirectory, String.format("%s.extras.fa", genomeRef.getRefVer()));
                    logger.info(combinedFastaExtrasFile.getAbsolutePath());

                    try (FileWriter combinedFastaFW = new FileWriter(combinedFastaFile);
                            BufferedWriter combinedFastaBW = new BufferedWriter(combinedFastaFW);
                            FileWriter combinedFastaExtrasFW = new FileWriter(combinedFastaExtrasFile);
                            BufferedWriter combinedFastaExtrasBW = new BufferedWriter(combinedFastaExtrasFW)) {

                        for (String key : indexSet) {
                            logger.info(key);

                            ReferenceSequence referenceSequence = gerese4jBuild.getReferenceSequence(key, false);

                            String seqType = null;

                            String header = referenceSequence.getHeader();

                            String[] idParts = header.split("\\|");

                            String desc = idParts[4];

                            if (desc.contains("chromosome")) {

                                if (desc.contains("alternate")) {
                                    seqType = "Alternate Loci";
                                } else if (desc.contains("NOVEL PATCH")) {
                                    seqType = "Novel Patch";
                                } else if (desc.contains("FIX PATCH")) {
                                    seqType = "Fix Patch";
                                } else if (desc.contains("unlocalized") || (desc.contains("genomic contig"))) {
                                    seqType = "Unlocalized Contig";
                                } else {
                                    seqType = "Chromosome";
                                }

                            } else {

                                if (desc.contains("mitochondrion")) {
                                    seqType = "Mitochondrial Genome";
                                } else {
                                    seqType = "Unplaced Contig";
                                }

                            }

                            if (nonExtraSequenceTypes.contains(seqType)) {
                                combinedFastaBW.write(String.format(">%s", referenceSequence.getHeader()));
                                combinedFastaBW.newLine();
                                combinedFastaBW.flush();
                                for (String line : Splitter.fixedLength(70).split(referenceSequence.getSequence().toString())) {
                                    combinedFastaBW.write(line);
                                    combinedFastaBW.newLine();
                                    combinedFastaBW.flush();
                                }
                            } else {
                                combinedFastaExtrasBW.write(String.format(">%s", referenceSequence.getHeader()));
                                combinedFastaExtrasBW.newLine();
                                combinedFastaExtrasBW.flush();
                                for (String line : Splitter.fixedLength(70).split(referenceSequence.getSequence().toString())) {
                                    combinedFastaExtrasBW.write(line);
                                    combinedFastaExtrasBW.newLine();
                                    combinedFastaExtrasBW.flush();
                                }
                            }

                        }

                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                gerese4jBuild.getReferenceSequenceCache().clear();

            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        long end = System.currentTimeMillis();
        logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000));

        return null;
    }

}
