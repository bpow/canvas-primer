package org.renci.canvas.primer.refseq.commands;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.gff3.GFF3Manager;
import org.renci.gff3.filters.GFF3AttributeValueFilter;
import org.renci.gff3.model.GFF3Record;

public class Scratch {

    @Test
    public void zxcv() {

        List<Pair<String, String>> rangeList = new ArrayList<>();

        Pattern locationPattern = Pattern.compile("(?<start>\\d+)\\.+?(?<stop>\\d+)?");

        String location = "order(917..931,941..943,980..982,986..988,1082..1084,1130..1141,1145..1150,1157..1159,1268..1270,1274..1285,1289..1291,1319..1324,1331..1333,1361..1372,1376..1378,1466..1468,1493..1495)";

        List<String> ranges = Arrays.asList(location.substring(7, location.length() - 1).split(","));
        for (String range : ranges) {
            Matcher m = locationPattern.matcher(range);
            m.find();
            if (m.matches()) {
                rangeList.add(Pair.of(m.group("start"), m.group("stop")));
            }
        }

    }

    @Test
    public void test() {

        String asdf = "1..256";
        Pattern p = Pattern.compile("(<start>\\d+)\\.+?(<end>\\d+)?");
        Matcher m = p.matcher(asdf);
        if (m.find()) {
            assertTrue(m.group("start").equals("1"));
            assertTrue(m.group("end").equals("256"));
        }

        asdf = "1";
        m = p.matcher(asdf);
        if (m.find()) {
            assertTrue(m.group("start").equals("1"));
            assertTrue(m.group("end") == null);
        }

    }

    @Test
    public void asdf() {
        GenomeRef a = new GenomeRef();
        a.setId(1);
        GenomeRef b = new GenomeRef();
        b.setId(2);
        GenomeRef c = new GenomeRef();
        c.setId(3);
        GenomeRef d = new GenomeRef();
        d.setId(4);
        GenomeRef latest = Arrays.asList(a, b, c, d).stream().max((x, y) -> x.getId().compareTo(y.getId())).get();
        System.out.println(latest.getId());
    }

    @Test
    public void testAlignmentParsing() {
        Map<String, List<GFF3Record>> map = new HashMap<>();

        GFF3Manager gff3Mgr = GFF3Manager.getInstance();

        // new File("/tmp/refseq/mappings", "GCF_000001405.28_knownrefseq_alignments.gff3"),
        for (File alignmentFile : Arrays.asList(new File("/tmp/refseq/mappings", "GCF_000001405.28_modelrefseq_alignments.gff3"))) {

            // List<GFF3Record> results = gff3Mgr.deserialize(alignmentFile, new GFF3AttributeValueFilter("Target", "NM_001317111.1"));
            List<GFF3Record> results = gff3Mgr.deserialize(alignmentFile, new GFF3AttributeValueFilter("Target", "XM_011509155.1"));
            if (CollectionUtils.isNotEmpty(results)) {
                for (GFF3Record record : results) {
                    if (!map.containsKey(record.getSequenceId())) {
                        map.put(record.getSequenceId(), new ArrayList<>());
                    }
                    map.get(record.getSequenceId()).add(record);
                }
            }
        }

        for (String sequenceId : map.keySet()) {
            System.out.println(sequenceId);
            List<GFF3Record> records = map.get(sequenceId);
            String strand = records.stream().map(a -> a.getStrand().getSymbol()).distinct().collect(Collectors.joining());
            System.out.println(strand);
            String identity = records.stream().map(a -> a.getAttributes().get("identity")).distinct().collect(Collectors.joining());

            if (StringUtils.isNotEmpty(identity) && !"null".equals(identity)) {
                System.out.println(Double.valueOf(identity) * 100D);
            } else {
                identity = records.stream().map(a -> a.getAttributes().get("pct_identity_gap")).distinct().collect(Collectors.joining());
                System.out.println(Double.valueOf(identity));
            }

        }

    }

}
