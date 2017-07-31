package org.renci.canvas.primer.dbsnp;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.renci.canvas.primer.commons.FTPFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;

public class Scratch {

    @Test
    public void scratch() {
        File latestVCF = new File("/home/jdr0887/Downloads/dbsnp", "00-All.vcf.gz");
        try (VCFFileReader vcfFileReader = new VCFFileReader(latestVCF, false)) {
            VCFHeader header = vcfFileReader.getFileHeader();
            Collection<VCFHeaderLine> headerLines = header.getOtherHeaderLines();
            headerLines.stream().forEach(a -> System.out.println(a.getKey()));
        }
    }

    @Test
    public void testBuildVersion() {
        Pattern p = Pattern.compile("human_9606_b(?<buildVersion>\\d+)_GRCh(?<refseqVersion>\\d+)p\\d+");

        List<String> buildDirectories = FTPFactory.ncbiListRemoteFiles("/snp/organisms/", "human_9606_b");
        List<Pair<Integer, Integer>> buildVersions = new ArrayList<>();
        buildDirectories.stream().forEach(a -> {
            Matcher m = p.matcher(a);
            m.find();
            if (m.matches()) {
                buildVersions.add(Pair.of(Integer.valueOf(m.group("buildVersion")), Integer.valueOf(m.group("refseqVersion"))));
            }
        });

        Integer latestBuildVersion = buildVersions.stream().max((a, b) -> a.getLeft().compareTo(b.getLeft())).map(a -> a.getKey()).get();

        List<Pair<Integer, Integer>> filteredPairs = buildVersions.stream().filter(a -> a.getLeft().equals(latestBuildVersion))
                .collect(Collectors.toList());

        for (Pair<Integer, Integer> pair : filteredPairs) {

            System.out.println(pair);

        }

    }

    @Test
    public void testJSON() throws Exception {

        JsonFactory jfactory = new JsonFactory();

        FileInputStream fis = new FileInputStream(new File("/home/jdr0887/Downloads", "refsnp-chr21.json"));
        JsonParser jParser = jfactory.createParser(fis);

        while (jParser.nextToken() != JsonToken.END_OBJECT) {

            String fieldname = jParser.getCurrentName();
            if ("update_build_id".equals(fieldname)) {
                jParser.nextToken();
                System.out.println(jParser.getText());
            }

        }
    }

}
