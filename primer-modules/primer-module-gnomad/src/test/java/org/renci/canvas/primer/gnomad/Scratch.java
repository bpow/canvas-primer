package org.renci.canvas.primer.gnomad;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class Scratch {

    @Test
    public void scratch() {

        File rootDir = new File("/tmp/NCNEXUS");

        for (int i = 1; i < 44; i++) {

            File listVersionDirectory = new File(rootDir, i + "");

            if (listVersionDirectory.exists()) {

                Arrays.asList(listVersionDirectory.listFiles(a -> a.getName().endsWith(".interval_list"))).forEach(a -> {

                    try {

                        List<String> newLines = new LinkedList<>();
                        List<String> lines = FileUtils.readLines(a, "UTF-8");
                        lines.forEach(line -> {
                            if (!line.startsWith("@")) {
                                String[] split = line.split("\t");
                                Integer startPosition = Integer.valueOf(split[1]);
                                Integer newStartPosition = startPosition - 1;
                                line = line.replace(startPosition.toString(), newStartPosition.toString());
                            }
                            newLines.add(line);
                        });
                        a.delete();
                        FileUtils.writeLines(new File(listVersionDirectory, a.getName()), newLines);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });

            }

        }

    }

    @Test
    public void downloadVersionRegex() {

        List<String> fileNames = Arrays.asList("gnomad.exomes.r2.0.1.sites.vcf.gz", "gnomad.exomes.r2.0.1.sites.X.vcf");

        Pattern p = Pattern.compile("gnomad\\.exomes\\.r(?<version>\\d\\.\\d\\.\\d)\\.sites\\.?(\\d+|X|Y)?\\.vcf\\.?(gz)?");

        for (String file : fileNames) {
            Matcher m = p.matcher(file);
            m.find();
            assertTrue(m.matches());
            String version = m.group("version");
            assertTrue(version.equals("2.0.1"));
        }

    }

    @Test
    public void fragmentation() throws Exception {

        File download = new File("/home/jdr0887/Downloads/gnomad", "gnomad.exomes.r2.0.1.sites.20.vcf");

        List<VariantContext> variantContextList = new ArrayList<>();

        try (VCFFileReader vcfFileReader = new VCFFileReader(download, false)) {

            for (VariantContext variantContext : vcfFileReader) {
                variantContextList.add(variantContext);
                if ((variantContextList.size() % 10000) == 0) {
                    try (FileOutputStream fos = new FileOutputStream(
                            new File("/tmp", String.format("%s.txt", UUID.randomUUID().toString())));
                            GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                            ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                        oos.writeObject(variantContextList);
                    }
                    variantContextList.clear();
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(new File("/tmp", String.format("%s.txt", UUID.randomUUID().toString())));
                GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
            oos.writeObject(variantContextList);
        }

    }

}
