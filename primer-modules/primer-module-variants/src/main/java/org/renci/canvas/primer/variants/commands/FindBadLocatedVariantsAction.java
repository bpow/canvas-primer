package org.renci.canvas.primer.variants.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.Executors;

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

    @Option(name = "--output", required = true, multiValued = false)
    private String output;

    public FindBadLocatedVariantsAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();
            try {
                File outputFile = new File(output);
                outputFile.getParentFile().mkdirs();

                try (FileWriter fw = new FileWriter(outputFile); BufferedWriter bw = new BufferedWriter(fw)) {
                    List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO().findBad(genomeRefId);
                    for (LocatedVariant locatedVariant : foundLocatedVariants) {
                        bw.write(String.format("%s%n", locatedVariant.toString()));
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
