package org.renci.canvas.primer.gnomad;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

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

        File download = new File("/home/jdr0887/Downloads", "gnomad.exomes.r2.0.1.sites.vcf");

        Pattern p = Pattern.compile("gnomad\\.exomes\\.r(?<version>\\d\\.\\d\\.\\d)\\.sites\\.vcf");
        Matcher m = p.matcher(download.getName());
        m.find();
        if (m.matches()) {
            String version = m.group("version");
            System.out.println(version);
        }

    }

}
