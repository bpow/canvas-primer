package org.renci.canvas.primer.gr.commands;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "genome-ref", name = "download", description = "Download Human Genome Reference files")
@Service
public class DownloadAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private PrimerDAOBeanService annotationDAOBeanService;

    public DownloadAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Path outputPath = Paths.get(System.getProperty("karaf.data"), "tmp", "GenomeReference");
        File outputDir = outputPath.toFile();
        outputDir.mkdirs();

        File readme = FTPFactory.ncbiDownload(outputDir, "/genomes/H_sapiens", "README_CURRENT_RELEASE");
        logger.info("Downloaded readme to: {}", readme.getAbsolutePath());

        List<String> lines = FileUtils.readLines(readme, "UTF-8");

        String fullVersion = "";
        Optional<String> assemblyNameLine = lines.stream().filter(a -> a.startsWith("ASSEMBLY NAME")).findAny();
        if (assemblyNameLine.isPresent()) {

            String[] lineSplit = assemblyNameLine.get().replace("ASSEMBLY NAME:", "").replace("GRCh", "").trim().split("\\.");
            String number = lineSplit[0];
            String version = lineSplit[1].contains("p") ? lineSplit[1].replaceAll("p", "") : lineSplit[1];
            if (StringUtils.isEmpty(version)) {
                version = "1";
            }
            fullVersion = String.format("BUILD.%s.%s", number, version);
        }

        if (StringUtils.isEmpty(fullVersion)) {
            logger.error("fullVersion is empty");
            return null;
        }

        logger.info("fullVersion: {}", fullVersion);

        outputPath = Paths.get(System.getProperty("karaf.data"), "tmp", "GenomeReference", fullVersion);
        outputDir = outputPath.toFile();
        outputDir.mkdirs();

        List<File> pulledFiles = FTPFactory.ncbiDownloadFiles(outputDir, "/genomes/H_sapiens/Assembled_chromosomes/seq", "hs_ref_", ".fa.gz");
        pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(outputDir, "/genomes/H_sapiens/CHR_Un", "hs_ref_", ".fa.gz"));
        pulledFiles.addAll(FTPFactory.ncbiDownloadFiles(outputDir, "/genomes/H_sapiens/CHR_MT", "hs_ref_", ".fa.gz"));

        // check_and_recover(pulledfiles,f,['unplaced','chrUn'],'CHR_Un')
        // check_and_recover(pulledfiles,f,['MT'],'CHR_MT')
        // f.close()
        // check_and_fill_unlocalized(pulledfiles)
        // check_and_fill_unlocalized([])

        return null;
    }

}
