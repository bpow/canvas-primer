package org.renci.canvas.primer.refseq.commands;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.gff3.GFF3Manager;
import org.renci.gff3.filters.GFF3AttributeValueFilter;
import org.renci.gff3.model.GFF3Record;

public class Scratch {

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
        List<GFF3Record> records = new ArrayList<>();

        for (File alignmentFile : Arrays.asList(new File("/tmp", "GCF_000001405.28_knownrefseq_alignments.gff3"),
                new File("/tmp", "GCF_000001405.28_modelrefseq_alignments.gff3"))) {
            GFF3Manager gff3Mgr = GFF3Manager.getInstance(alignmentFile);

            List<GFF3Record> results = gff3Mgr.deserialize(new GFF3AttributeValueFilter("Target", "NM_001267550.2"));
            if (CollectionUtils.isNotEmpty(results)) {
                results.forEach(records::add);
            }
        }

        String sequenceId = records.stream().map(a -> a.getSequenceId()).distinct().collect(Collectors.joining());
        System.out.println(sequenceId);
        String strand = records.stream().map(a -> a.getStrand().getSymbol()).distinct().collect(Collectors.joining());
        System.out.println(strand);
        String identity = records.stream().map(a -> a.getAttributes().get("identity")).distinct().collect(Collectors.joining());
        System.out.println(Double.valueOf(identity) * 100D);

    }

}
