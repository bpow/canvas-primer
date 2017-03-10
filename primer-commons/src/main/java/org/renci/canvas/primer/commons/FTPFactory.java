package org.renci.canvas.primer.commons;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FTPFactory {

    private static final Logger logger = LoggerFactory.getLogger(FTPFactory.class);

    public static File download(File outputDir, String host, String path, String name) {
        logger.info("downloading: {}", String.format("%s:%s/%s", host, path, name));
        File ret = new File("/tmp", name);
        if (ret.exists()) {
            return ret;
        }
        ret = new File(outputDir, name);
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(host);
            ftpClient.login("anonymous", "anonymous");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                logger.error("FTP server refused connection.");
                return null;
            }
            try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(ret))) {
                ftpClient.retrieveFile(String.format("%s/%s", path, name), fos);
                fos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static List<File> downloadFiles(File outputDir, String host, String path, String prefix, String suffix) {

        List<File> ret = new ArrayList<File>();

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(host);

            ftpClient.login("anonymous", "anonymous");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                logger.error("FTP server refused connection.");
                return null;
            }

            List<FTPFile> ftpFileList = Arrays
                    .asList(ftpClient.listFiles(path, a -> a.getName().startsWith(prefix) && a.getName().endsWith(suffix)));

            for (FTPFile ftpFile : ftpFileList) {
                File tmpFile = new File(outputDir, ftpFile.getName());
                if (tmpFile.exists()) {
                    ret.add(tmpFile);
                    continue;
                }

                tmpFile = new File(outputDir, ftpFile.getName());
                logger.info("downloading: {}", ftpFile.getName());
                try (FileOutputStream fos = new FileOutputStream(tmpFile); BufferedOutputStream os = new BufferedOutputStream(fos)) {
                    ftpClient.retrieveFile(String.format("%s/%s", path, ftpFile.getName()), fos);
                    fos.flush();
                    ret.add(tmpFile);
                } catch (Exception e) {
                    logger.error("Error", e);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static List<String> listRemoteFiles(String host, String path, String prefix, String suffix) {

        List<String> ret = new ArrayList<>();

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(host);

            ftpClient.login("anonymous", "anonymous");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                logger.error("FTP server refused connection.");
                return null;
            }

            List<FTPFile> ftpFileList = null;
            if (StringUtils.isNotEmpty(prefix) && StringUtils.isNotEmpty(suffix)) {
                ftpFileList = Arrays.asList(ftpClient.listFiles(path, a -> a.getName().startsWith(prefix) && a.getName().endsWith(suffix)));
            } else if (StringUtils.isNotEmpty(prefix) && StringUtils.isEmpty(suffix)) {
                ftpFileList = Arrays.asList(ftpClient.listFiles(path, a -> a.getName().startsWith(prefix)));
            } else if (StringUtils.isEmpty(prefix) && StringUtils.isNotEmpty(suffix)) {
                ftpFileList = Arrays.asList(ftpClient.listFiles(path, a -> a.getName().endsWith(suffix)));
            }

            if (CollectionUtils.isNotEmpty(ftpFileList)) {
                for (FTPFile ftpFile : ftpFileList) {
                    ret.add(ftpFile.getName());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static List<String> ncbiListRemoteFiles(String path, String prefix, String suffix) {
        return listRemoteFiles("ftp.ncbi.nlm.nih.gov", path, prefix, suffix);
    }

    public static List<String> ncbiListRemoteFiles(String path, String prefix) {
        return listRemoteFiles("ftp.ncbi.nlm.nih.gov", path, prefix, null);
    }

    public static List<File> ncbiDownloadFiles(File outputDir, String path, String prefix, String suffix) {
        return downloadFiles(outputDir, "ftp.ncbi.nlm.nih.gov", path, prefix, suffix);
    }

    public static File ncbiDownload(File outputDir, String path, String name) {
        return download(outputDir, "ftp.ncbi.nlm.nih.gov", path, name);
    }

    public static File ucscDownload(File outputDir, String path, String name) {
        String host = "hgdownload.cse.ucsc.edu";
        return download(outputDir, host, path, name);
    }
}
