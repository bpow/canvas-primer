package org.renci.canvas.primer.variants.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_38_7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "variants", name = "find-not-in-reference", description = "")
@Service
public class FindLocatedVariantsNotInReferenceAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(FindLocatedVariantsNotInReferenceAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Option(name = "--genomeRefId", required = true, multiValued = false)
    private Integer genomeRefId;

    @Option(name = "--variantType", required = true, multiValued = false)
    private String variantType;

    @Option(name = "--outputDirectory", required = true, multiValued = false)
    private String outputDirectory;

    public FindLocatedVariantsNotInReferenceAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        List<GeReSe4jBuild> gerese4jBuilds = Arrays.asList(GeReSe4jBuild_37_3.getInstance(), GeReSe4jBuild_38_7.getInstance());

        VariantType vType = canvasDAOBeanService.getVariantTypeDAO().findById(variantType);

        File outputDir = new File(outputDirectory);
        outputDir.mkdirs();

        File output = new File(outputDir, String.format("badLocatedVariantCoordinates-%s.txt", genomeRefId.toString()));

        for (GeReSe4jBuild build : gerese4jBuilds) {
            build.init();
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();

            try (FileWriter fw = new FileWriter(output); BufferedWriter bw = new BufferedWriter(fw)) {

                String header = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "LocatedVariant",
                        String.format("Variants_?_%s", genomeRefId.toString()), "BinResultsFinalDiagnostic", "ClinVar", "Gnomad", "HGMD",
                        "ExAC", "ESP", "dbSNP", "OneKGenomeSNP", "OneKGenomeIndel", "Assembly");

                bw.write(header);
                bw.newLine();
                bw.flush();

                List<Long> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findIdByGenomeRefIdAndVariantType(genomeRefId,
                        vType.getId());

                if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                    logger.warn("foundLocatedVariants is empty");
                    return;
                }

                logger.info("foundLocatedVariants.size(): {}", foundLocatedVariants.size());

                List<List<Long>> partitionedFoundLocatedVariants = ListUtils.partition(foundLocatedVariants, 180);

                for (List<Long> partitionList : partitionedFoundLocatedVariants) {

                    logger.info("starting on partition {}/{}", partitionedFoundLocatedVariants.indexOf(partitionList) + 1,
                            partitionedFoundLocatedVariants.size());

                    List<Future<String>> results = new ArrayList<>();

                    List<LocatedVariant> locatedVariantList = canvasDAOBeanService.getLocatedVariantDAO().findByIdList(partitionList);

                    ExecutorService es = Executors.newFixedThreadPool(3);
                    for (LocatedVariant locatedVariant : locatedVariantList) {

                        Future<String> result = es.submit(() -> {

                            try {

                                GeReSe4jBuild build = null;
                                if (locatedVariant.getGenomeRef().getName().startsWith("37")) {
                                    build = gerese4jBuilds.get(0);
                                } else {
                                    build = gerese4jBuilds.get(1);
                                }

                                String refSeqValue = build.getRegion(locatedVariant.getGenomeRefSeq().getId(),
                                        Range.between(locatedVariant.getPosition(), locatedVariant.getEndPosition()),
                                        !vType.getId().equals("ins"), false);

                                if (!refSeqValue.equals(locatedVariant.getRef())) {

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
                                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getVariants_61_2_DAO()
                                                    .findByLocatedVariantId(locatedVariant.getId()))) {
                                                foundInVariants = Boolean.TRUE;
                                            }
                                            break;
                                        case 4:
                                            if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getVariants_80_4_DAO()
                                                    .findByLocatedVariantId(locatedVariant.getId()))) {
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

                                    if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getGnomADVariantFrequencyDAO()
                                            .findByLocatedVariantId(locatedVariant.getId()))) {
                                        foundInGnomad = Boolean.TRUE;
                                    }

                                    if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getHGMDLocatedVariantDAO()
                                            .findByLocatedVariantId(locatedVariant.getId()))) {
                                        foundInHGMD = Boolean.TRUE;
                                    }

                                    if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getExACVariantFrequencyDAO()
                                            .findByLocatedVariantId(locatedVariant.getId()))) {
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

                                    if (CollectionUtils.isNotEmpty(canvasDAOBeanService.getAssemblyLocatedVariantDAO()
                                            .findByLocatedVariantId(locatedVariant.getId()))) {
                                        foundInAssembly = Boolean.TRUE;
                                    }

                                    return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", locatedVariant.toString(),
                                            foundInVariants, foundInBinResultsFinalDiagnostic, foundInClinVar, foundInGnomad, foundInHGMD,
                                            foundInExAC, foundInESP, foundInDBSNP, foundInOneKGenomeSNP, foundInOneKGenomeIndel,
                                            foundInAssembly);
                                }
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                            return "";

                        });

                        results.add(result);
                    }

                    for (Future<String> result : results) {

                        String ret = result.get();
                        if (StringUtils.isNotEmpty(ret)) {
                            bw.write(ret);
                            bw.newLine();
                            bw.flush();
                        }

                    }

                    es.shutdown();
                    if (es.awaitTermination(1, TimeUnit.HOURS)) {
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
