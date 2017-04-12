package org.renci.canvas.primer.gr.commands;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.BundleContext;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "genome-reference", name = "create-combined-fasta-file", description = "Combine fasta files")
@Service
public class CreateCombinedFastaFileAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(CreateCombinedFastaFileAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private BundleContext bundleContext;

    @Option(name = "--outputDirectory", required = false, multiValued = false)
    private String outputDirectory;

    public CreateCombinedFastaFileAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        File outputDir = null;
        if (StringUtils.isEmpty(outputDirectory)) {
            Path outputPath = Paths.get(System.getProperty("karaf.data"), "GenomeReference");
            outputDir = outputPath.toFile();
            outputDir.mkdirs();
        } else {
            outputDir = new File(outputDirectory);
        }

        Executors.newSingleThreadExecutor().submit(new CreateCombinedFastaFileCallable(outputDir, canvasDAOBeanService));
        return null;
    }

}
