package org.renci.canvas.primer.clinvar.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class Scratch {

    @Test
    public void test() {

        List<Pair<Integer, Integer>> versionPairList = new ArrayList<>();
        versionPairList.add(Pair.of(1, 1));
        versionPairList.add(Pair.of(1, 2));
        versionPairList.add(Pair.of(1, 41));
        versionPairList.add(Pair.of(1, 42));
        // versionPairList.add(Pair.of(2, 1));
        // versionPairList.add(Pair.of(2, 4));

        versionPairList.sort((a, b) -> {
            int ret = b.getLeft().compareTo(a.getLeft());
            if (ret == 0) {
                ret = b.getRight().compareTo(a.getRight());
            }
            return ret;
        });
        System.out.println(versionPairList.get(0).getLeft());
        System.out.println(versionPairList.get(0).getRight());
    }

}
