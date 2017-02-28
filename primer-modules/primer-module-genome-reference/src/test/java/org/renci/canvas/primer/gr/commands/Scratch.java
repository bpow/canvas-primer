package org.renci.canvas.primer.gr.commands;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class Scratch {

    @Test
    public void scratch() throws Exception {
        File readme = new File("/tmp", "README_CURRENT_RELEASE");

        List<String> lines = FileUtils.readLines(readme, "UTF-8");

        String fullVersion = "";
        Optional<String> assemblyNameLine = lines.stream().filter(a -> a.startsWith("ASSEMBLY NAME")).findAny();
        if (assemblyNameLine.isPresent()) {

            String line = assemblyNameLine.get().replace("ASSEMBLY NAME:", "").replace("GRCh", "").trim();
            String[] lineSplit = line.split("\\.");
            String number = lineSplit[0];
            String version = lineSplit[1].contains("p") ? lineSplit[1].replaceAll("p", "") : lineSplit[1];
            if (StringUtils.isEmpty(version)) {
                version = "1";
            }
            fullVersion = String.format("BUILD.%s.%s", number, version);
        }

    }
}
