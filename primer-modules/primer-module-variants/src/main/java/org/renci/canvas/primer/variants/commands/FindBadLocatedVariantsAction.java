package org.renci.canvas.primer.variants.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "variants", name = "find-bad", description = "")
@Service
public class FindBadLocatedVariantsAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(FindBadLocatedVariantsAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Option(name = "--genomeRefId", required = true, multiValued = false)
    private Integer genomeRefId;

    @Option(name = "--outputDirectory", required = true, multiValued = false)
    private String outputDirectory;

    public FindBadLocatedVariantsAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();
            try {

                File outputDir = new File(outputDirectory);
                outputDir.mkdirs();

                File outputFile = new File(outputDir, String.format("badVariants-%s.txt", genomeRefId.toString()));

                List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findBad(genomeRefId);
                logger.info("foundLocatedVariants.size(): {}", foundLocatedVariants.size());

                ExecutorService es = Executors.newFixedThreadPool(3);

                List<Future<String>> results = new ArrayList<>();

                for (LocatedVariant locatedVariant : foundLocatedVariants) {

                    Future<String> result = es.submit(() -> {

                        try {
                            Boolean foundInVariants = Boolean.FALSE;
                            Boolean foundInBinResultsFinalDiagnostic = Boolean.FALSE;
                            Boolean foundInClinVar = Boolean.FALSE;
                            Boolean foundInGnomad = Boolean.FALSE;
                            Boolean foundInHGMD = Boolean.FALSE;
                            Boolean foundInExAC = Boolean.FALSE;
                            Boolean foundInESP = Boolean.FALSE;
                            Boolean foundInDBSNP = Boolean.FALSE;
                            Boolean foundInOneKGenomeSNP = Boolean.FALSE;
                            Boolean foundInOneKGenomeIndel = Boolean.FALSE;
                            Boolean foundInAssembly = Boolean.FALSE;

                            switch (genomeRefId) {
                                case 2:
                                    if (CollectionUtils.isNotEmpty(
                                            canvasDAOBeanService.getVariants_61_2_DAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                        foundInVariants = Boolean.TRUE;
                                    }
                                    break;
                                case 4:
                                    if (CollectionUtils.isNotEmpty(
                                            canvasDAOBeanService.getVariants_80_4_DAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                        foundInVariants = Boolean.TRUE;
                                    }
                                    break;

                            }

                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getBinResultsFinalDiagnosticDAO()
                                    .findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInGnomad = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getReferenceClinicalAssertionDAO()
                                    .findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInClinVar = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(
                                    canvasDAOBeanService.getGnomADVariantFrequencyDAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInGnomad = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(
                                    canvasDAOBeanService.getHGMDLocatedVariantDAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInHGMD = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(
                                    canvasDAOBeanService.getExACVariantFrequencyDAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInExAC = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getESPSNPFrequencyPopulationDAO()
                                    .findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInESP = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(
                                    canvasDAOBeanService.getSNPMappingAggDAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInDBSNP = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getOneKGenomesSNPFrequencyPopulationDAO()
                                    .findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInOneKGenomeSNP = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getOneKGenomesIndelFrequencyDAO()
                                    .findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInOneKGenomeIndel = Boolean.TRUE;
                            }

                            if (CollectionUtils.isNotEmpty(
                                    canvasDAOBeanService.getAssemblyLocatedVariantDAO().findByLocatedVariantId(locatedVariant.getId()))) {
                                foundInAssembly = Boolean.TRUE;
                            }

                            return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", locatedVariant.getId(), foundInVariants,
                                    foundInBinResultsFinalDiagnostic, foundInClinVar, foundInGnomad, foundInHGMD, foundInExAC, foundInESP,
                                    foundInDBSNP, foundInOneKGenomeSNP, foundInOneKGenomeIndel, foundInAssembly);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        return "";
                    });

                    results.add(result);

                }

                try (FileWriter fw = new FileWriter(outputFile); BufferedWriter bw = new BufferedWriter(fw)) {

                    String header = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "LocatedVariant.id",
                            String.format("Variants_?_%s", genomeRefId.toString()), "BinResultsFinalDiagnostic", "ClinVar", "Gnomad",
                            "HGMD", "ExAC", "ESP", "dbSNP", "OneKGenomeSNP", "OneKGenomeIndel", "Assembly");

                    bw.write(header);
                    bw.newLine();
                    bw.flush();

                    for (Future<String> result : results) {
                        bw.write(result.get());
                        bw.newLine();
                        bw.flush();
                    }

                    es.shutdown();
                    if (!es.awaitTermination(1L, TimeUnit.DAYS)) {
                        es.shutdownNow();
                    }
                }

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            long end = System.currentTimeMillis();
            logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000));
        });

        return null;
    }

}
