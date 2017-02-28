package org.renci.canvas.primer.esp.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "esp", name = "download", description = "Download ESP")
@Service
public class DownloadAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private PrimerDAOBeanService annotationDAOBeanService;

    @Option(name = "--espFileName", description = "From http://evs.gs.washington.edu/EVS/", required = false, multiValued = false)
    private String espFileName = "ESP6500SI-V2-SSA137.GRCh38-liftover.snps_indels.vcf.tar.gz";

    public DownloadAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String sourceDownload = String.format("http://evs.gs.washington.edu/evs_bulk_data/%s", this.espFileName);
                URL url = new URL(sourceDownload);
                Path destinationParentPath = Paths.get(System.getProperty("karaf.data"), "tmp", "ESP");
                logger.info("download url: {}", sourceDownload);
                File destinationParentFile = destinationParentPath.toFile();
                destinationParentFile.mkdirs();
                File downloadDestination = new File(destinationParentFile, this.espFileName);
                FileUtils.copyURLToFile(url, downloadDestination);
                logger.info("downloaded to: {}", downloadDestination.getAbsolutePath());

                List<File> entryFileList = new ArrayList<>();
                BufferedReader br = null;
                try (FileInputStream fis = new FileInputStream(downloadDestination);
                        GZIPInputStream gin = new GZIPInputStream(fis);
                        TarArchiveInputStream tar = new TarArchiveInputStream(gin)) {
                    TarArchiveEntry entry;
                    while ((entry = tar.getNextTarEntry()) != null) {
                        logger.info("entry.getName(): {}", entry.getName());
                        br = new BufferedReader(new InputStreamReader(tar));
                        File entryFile = new File(destinationParentFile, entry.getName());
                        try (FileWriter fw = new FileWriter(entryFile); BufferedWriter bw = new BufferedWriter(fw)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                fw.write(String.format("%s%n", line));
                                fw.flush();
                            }
                        }
                        entryFileList.add(entryFile);
                    }
                }

                if (br != null) {
                    br.close();
                }

                downloadDestination.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return null;
    }

    public String getEspFileName() {
        return espFileName;
    }

    public void setEspFileName(String espFileName) {
        this.espFileName = espFileName;
    }

}
