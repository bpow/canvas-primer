package org.renci.canvas.primer.gr.commands;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.base.Splitter;

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

    @Test
    public void test() {
        String asdf = "GATCACAGGTCTATCACCCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGG"
                + "GTGTGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTC"
                + "CTGCCTCATTCTATTATTTATCGCACCTACGTTCAATATTACAGGCGAACATACCTACTAAAGTGTGTTA"
                + "ATTAATTAATGCTTGTAGGACATAATAATAACAATTGAATGTCTGCACAGCCGCTTTCCACACAGACATC";

        System.out.println(StringUtils.split(asdf, null, 70)[0]);
        System.out.println(Splitter.fixedLength(70).split(asdf).iterator().next());

    }

    @Test
    public void testFileNamePattern() {
        Pattern fileNamePattern = Pattern.compile(
                "hs_ref_(?<genomeRefAccession>GRCh\\d+)\\.p(?<patch>\\d+)_(chr)?(?<chromosome>\\d+|MT|Un|X|Y|unlocalized|unplaced|alts)\\.fa\\.gz");

        for (String file : Arrays.asList("hs_ref_GRCh38.p7_chr4.fa.gz", "hs_ref_GRCh38.p7_chrX.fa.gz", "hs_ref_GRCh38.p7_chrY.fa.gz",
                "hs_ref_GRCh38.p7_unlocalized.fa.gz", "hs_ref_GRCh38.p7_unplaced.fa.gz")) {

            Matcher m = fileNamePattern.matcher(file);
            assertTrue(m.find());
            System.out.println(m.group("genomeRefAccession"));
            System.out.println(m.group("patch"));
            System.out.println(m.group("chromosome"));
        }

    }

    @Test
    public void testDescPattern() {
        Pattern fileNamePattern = Pattern
                .compile("Homo sapiens chromosome (?<contig>\\d+|X|Y).+(?<genomeRefAccession>GRCh\\d+)\\.p(?<patch>\\d+).+");
        for (String file : Arrays.asList("Homo sapiens chromosome 1 genomic patch of type FIX, GRCh38.p7 PATCHES HG1342_HG2282_PATCH",
                "Homo sapiens chromosome X genomic scaffold, GRCh38.p7 alternate locus group ALT_REF_LOCI_2 HSCHRX_2_CTG3",
                "Homo sapiens chromosome Y genomic patch of type FIX, GRCh38.p7 PATCHES HG2062_PATCH")) {
            Matcher m = fileNamePattern.matcher(file);
            assertTrue(m.find());
            System.out.println(m.group("contig"));
            System.out.println(m.group("genomeRefAccession"));
            System.out.println(m.group("patch"));
        }

    }

    @Test
    public void testParseFastaKey() {
        String asdf = ">gi|568815576|ref|NC_000022.11| Homo sapiens chromosome 22, GRCh38.p7 Primary Assembly";
        String[] asdfSplit = asdf.split("\\|");
        for (String a : asdfSplit) {
            System.out.println(a);
        }
    }

}
