package org.renci.canvas.primer.variants.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Range;
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

    @Option(name = "--output", required = true, multiValued = false)
    private String output;

    public FindLocatedVariantsNotInReferenceAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        List<GeReSe4jBuild> gerese4jBuilds = Arrays.asList(GeReSe4jBuild_37_3.getInstance(), GeReSe4jBuild_38_7.getInstance());

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();

            try {

                VariantType vType = canvasDAOBeanService.getVariantTypeDAO().findById(variantType);

                File outputFile = new File(output);
                outputFile.getParentFile().mkdirs();

                try (FileWriter fw = new FileWriter(outputFile); BufferedWriter bw = new BufferedWriter(fw)) {

                    List<Long> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                            .findIdByGenomeRefIdAndVariantType(genomeRefId, vType.getId());

                    List<List<Long>> partitionedFoundLocatedVariants = ListUtils.partition(foundLocatedVariants, 10);

                    for (List<Long> locatedVariantIdList : partitionedFoundLocatedVariants) {
                        logger.info("starting on partition %s/%s", partitionedFoundLocatedVariants.indexOf(locatedVariantIdList) + 1,
                                partitionedFoundLocatedVariants.size());

                        for (Long locatedVariantId : locatedVariantIdList) {
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
                                bw.write(String.format("%s%n", locatedVariant.toString()));
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
