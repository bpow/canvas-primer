package org.renci.canvas.primer.ncgenes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.renci.canvas.dao.clinbin.model.DiagnosticGene;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.renci.canvas.dao.refseq.model.TranscriptMaps;
import org.renci.canvas.dao.refseq.model.TranscriptMapsExons;

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
    public void test() {
        

    }
    
}
