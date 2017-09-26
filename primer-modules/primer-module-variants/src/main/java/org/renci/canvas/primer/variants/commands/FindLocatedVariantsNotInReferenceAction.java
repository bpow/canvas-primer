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

            try {

                try (FileWriter fw = new FileWriter(output); BufferedWriter bw = new BufferedWriter(fw)) {

                    List<Long> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                            .findIdByGenomeRefIdAndVariantType(genomeRefId, vType.getId());

                    if (CollectionUtils.isEmpty(foundLocatedVariants)) {
                        logger.warn("foundLocatedVariants is empty");
                        return;
                    }

                    logger.info("foundLocatedVariants.size(): {}", foundLocatedVariants.size());

                    List<List<Long>> partitionedFoundLocatedVariants = ListUtils.partition(foundLocatedVariants, 1000);

                    for (List<Long> partitionList : partitionedFoundLocatedVariants) {

                        logger.info("starting on partition {}/{}", partitionedFoundLocatedVariants.indexOf(partitionList) + 1,
                                partitionedFoundLocatedVariants.size());

                        List<Future<String>> results = new ArrayList<>();

                        ExecutorService es = Executors.newFixedThreadPool(4);
                        for (Long locatedVariantId : partitionList) {

                            Future<String> result = es.submit(() -> {

                                try {
                                    LocatedVariant locatedVariant = canvasDAOBeanService.getLocatedVariantDAO().findById(locatedVariantId);

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
                                        return locatedVariant.toString();
                                    }
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                                return "";

                            });

                            results.add(result);
                        }

                        es.shutdown();
                        if (es.awaitTermination(1, TimeUnit.DAYS)) {
                            es.shutdownNow();
                        }

                        for (Future<String> result : results) {

                            String ret = result.get();
                            if (StringUtils.isNotEmpty(ret)) {
                                bw.write(ret);
                                bw.newLine();
                                bw.flush();
                            }

                        }

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
