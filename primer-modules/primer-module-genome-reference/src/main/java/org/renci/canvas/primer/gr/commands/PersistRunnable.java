package org.renci.canvas.primer.gr.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.ref.model.GenomeRefSeqAlternateId;
import org.renci.canvas.dao.ref.model.GenomeRefSeqAlternateIdPK;
import org.renci.canvas.dao.ref.model.SequenceType;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.GeReSe4jException;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_38_7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistRunnable implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(PersistRunnable.class);

    private static final Pattern descriptionPattern = Pattern
            .compile("Homo sapiens chromosome (?<contig>\\d+|X|Y).+(?<genomeRefBuild>GRCh\\d+)\\.p(?<patch>\\d+).+");

    private CANVASDAOBeanService canvasDAOBeanService;

    public PersistRunnable(CANVASDAOBeanService canvasDAOBeanService) {
        super();
        this.canvasDAOBeanService = canvasDAOBeanService;
    }

    @Override
    public Void call() {

        long start = System.currentTimeMillis();
        try {

            List<GeReSe4jBuild> gerese4jBuilds = Arrays.asList(GeReSe4jBuild_37_3.getInstance(), GeReSe4jBuild_38_7.getInstance());

            logger.info("Loading SequenceTypes");
            List<String> stList = Arrays.asList("Alternate Loci", "Chromosome", "Fix Patch", "Mitochondrial Genome", "Novel Patch",
                    "Unlocalized Contig", "Unplaced Contig");
            List<SequenceType> seqTypeList = canvasDAOBeanService.getSequenceTypeDAO().findAll();
            for (String st : stList) {
                if (!seqTypeList.stream().filter(a -> a.getId().equals(st)).findAny().isPresent()) {
                    SequenceType sequenceType = new SequenceType(st);
                    canvasDAOBeanService.getSequenceTypeDAO().save(sequenceType);
                }
            }

            logger.info("Loading GenomeRef");
            List<GenomeRef> genomeRefList = canvasDAOBeanService.getGenomeRefDAO().findAll();
            if (CollectionUtils.isNotEmpty(genomeRefList)) {
                Integer id = genomeRefList.stream().map(GenomeRef::getId).max(Comparator.naturalOrder()).get();

                for (GeReSe4jBuild gerese4jBuild : gerese4jBuilds) {
                    Optional<GenomeRef> optionalGenomeRef = genomeRefList.stream()
                            .filter(a -> a.getRefVer().equals(String.format("BUILD.%s", gerese4jBuild.getBuild().getVersion()))).findAny();
                    if (!optionalGenomeRef.isPresent()) {
                        GenomeRef genomeRef = new GenomeRef("NCBI", String.format("BUILD.%s", gerese4jBuild.getBuild().getVersion()),
                                gerese4jBuild.getBuild().getVersion());
                        genomeRef.setId(++id);
                        canvasDAOBeanService.getGenomeRefDAO().save(genomeRef);
                    }
                }

            } else {
                AtomicInteger count = new AtomicInteger(0);
                for (GeReSe4jBuild gerese4jBuild : gerese4jBuilds) {
                    GenomeRef genomeRef = new GenomeRef("NCBI", String.format("BUILD.%s", gerese4jBuild.getBuild().getVersion()),
                            gerese4jBuild.getBuild().getVersion());
                    genomeRef.setId(count.incrementAndGet());
                    canvasDAOBeanService.getGenomeRefDAO().save(genomeRef);
                }
            }

            // refreshing list
            genomeRefList = canvasDAOBeanService.getGenomeRefDAO().findAll();

            List<SequenceType> allSequenceTypes = canvasDAOBeanService.getSequenceTypeDAO().findAll();

            for (GeReSe4jBuild gerese4jBuild : gerese4jBuilds) {

                logger.info("Loading GenomeRefSeq for {}", gerese4jBuild.getBuild().getVersion());
                Optional<GenomeRef> optionalGenomeRef = genomeRefList.stream()
                        .filter(a -> a.getName().equals(gerese4jBuild.getBuild().getVersion())).findAny();
                if (optionalGenomeRef.isPresent()) {
                    Set<String> indexSet = gerese4jBuild.getIndices();

                    Set<GenomeRefSeq> genomeRefSeqs = new HashSet<>();
                    for (String key : indexSet) {
                        String header = gerese4jBuild.getHeader(key);
                        GenomeRefSeq genomeRefSeq = persistGenomeRefSeqs(header, allSequenceTypes);
                        logger.info(genomeRefSeq.toString());
                        if (!genomeRefSeqs.contains(genomeRefSeq)) {
                            genomeRefSeqs.add(genomeRefSeq);
                        }
                    }

                    GenomeRef genomeRef = optionalGenomeRef.get();
                    genomeRef.getGenomeRefSeqs().addAll(Collections.synchronizedSet(genomeRefSeqs));
                    canvasDAOBeanService.getGenomeRefDAO().save(genomeRef);

                }

            }

        } catch (CANVASDAOException | GeReSe4jException e) {
            logger.error(e.getMessage(), e);
        }

        long end = System.currentTimeMillis();
        logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000));

        return null;
    }

    private GenomeRefSeq persistGenomeRefSeqs(String header, List<SequenceType> allSequenceTypes) throws CANVASDAOException {

        String genomeRefBuild = null;
        String contig = null;
        String patchVersion = null;

        SequenceType seqType = null;
        String[] idParts = header.split("\\|");

        String gi = idParts[1];
        String genomeRefAccession = idParts[3];
        String desc = idParts[4].trim();

        if (desc.contains("chromosome")) {

            Matcher m = descriptionPattern.matcher(desc);
            if (m.find()) {
                genomeRefBuild = m.group("genomeRefBuild");
                contig = m.group("contig");
                patchVersion = m.group("patch");
            }

            if (desc.contains("alternate")) {
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Alternate Loci")).findFirst().get();
            } else if (desc.contains("NOVEL PATCH")) {
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Novel Patch")).findFirst().get();
            } else if (desc.contains("FIX PATCH")) {
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Fix Patch")).findFirst().get();
            } else if (desc.contains("unlocalized") || (desc.contains("genomic contig"))) {
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Unlocalized Contig")).findFirst().get();
            } else {
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Chromosome")).findFirst().get();
            }
        } else {

            if (desc.contains("mitochondrion")) {
                contig = "M";
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Mitochondrial Genome")).findFirst().get();
            } else {
                contig = "Unplaced";
                seqType = allSequenceTypes.stream().filter(a -> a.getId().equals("Unplaced Contig")).findFirst().get();
            }

        }
        logger.info(seqType.toString());

        GenomeRefSeq genomeRefSeq = new GenomeRefSeq(genomeRefAccession, contig, desc);
        genomeRefSeq.setSequenceType(seqType);

        GenomeRefSeq foundGenomeRefSeq = canvasDAOBeanService.getGenomeRefSeqDAO().findById(genomeRefAccession);
        if (foundGenomeRefSeq != null) {
            genomeRefSeq = foundGenomeRefSeq;
        } else {
            canvasDAOBeanService.getGenomeRefSeqDAO().save(genomeRefSeq);
        }

        GenomeRefSeqAlternateIdPK genomeRefSeqAlternateIdKey = new GenomeRefSeqAlternateIdPK(genomeRefSeq.getId(), "gi");
        GenomeRefSeqAlternateId foundGenomeRefSeqAlternateId = canvasDAOBeanService.getGenomeRefSeqAlternateIdDAO()
                .findById(genomeRefSeqAlternateIdKey);
        if (foundGenomeRefSeqAlternateId == null) {
            GenomeRefSeqAlternateId genomeRefSeqAlternateId = new GenomeRefSeqAlternateId(genomeRefSeqAlternateIdKey);
            genomeRefSeqAlternateId.setGenomeRefSeq(genomeRefSeq);
            genomeRefSeqAlternateId.setIdentifier(gi);
            logger.info(genomeRefSeqAlternateId.toString());
            canvasDAOBeanService.getGenomeRefSeqAlternateIdDAO().save(genomeRefSeqAlternateId);
        }

        return genomeRefSeq;
    }

}
