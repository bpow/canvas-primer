package org.renci.canvas.primer.dbsnp.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.commons.LocatedVariantFactory;
import org.renci.canvas.dao.dbsnp.model.SNP;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.CanonicalAllele;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.canvas.primer.commons.PrimerException;
import org.renci.canvas.primer.commons.UpdateDiagnosticResultVersionCallable;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

@Command(scope = "dbsnp", name = "persist", description = "Persist dbSNP data")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Path dbsnpPath = Paths.get(System.getProperty("karaf.data"), "dbsnp");
        File dbsnpDir = dbsnpPath.toFile();
        File dbsnpTmpDir = new File(dbsnpDir, "tmp");

        if (!dbsnpTmpDir.exists()) {
            dbsnpTmpDir.mkdirs();
        }

        Executors.newSingleThreadExecutor().submit(() -> {

            try {

                Pattern p = Pattern.compile("human_9606_b(?<buildVersion>\\d+)_GRCh(?<refseqVersion>\\d+)p(?<patch>\\d+)");

                List<String> buildDirectories = FTPFactory.ncbiListRemoteFiles("/snp/organisms/", "human_9606_b");
                List<Triple<Integer, Integer, Integer>> buildVersions = new ArrayList<>();
                buildDirectories.stream().forEach(a -> {
                    Matcher m = p.matcher(a);
                    m.find();
                    if (m.matches()) {
                        buildVersions.add(Triple.of(Integer.valueOf(m.group("buildVersion")), Integer.valueOf(m.group("refseqVersion")),
                                Integer.valueOf(m.group("patch"))));
                    }
                });

                Integer latestBuildVersion = buildVersions.stream().max((a, b) -> a.getLeft().compareTo(b.getLeft())).map(a -> a.getLeft())
                        .get();

                Integer latestReferenceVersion = buildVersions.stream().max((a, b) -> a.getMiddle().compareTo(b.getMiddle()))
                        .map(a -> a.getMiddle()).get();

                List<Triple<Integer, Integer, Integer>> filteredTriples = buildVersions.stream()
                        .filter(a -> a.getLeft().equals(latestBuildVersion) && a.getMiddle().equals(latestReferenceVersion))
                        .collect(Collectors.toList());

                List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();
                List<VariantType> allVariantTypes = canvasDAOBeanService.getVariantTypeDAO().findAll();

                GenomeRef genomeRef37 = allGenomeRefs.stream().filter(a -> a.getId().equals(1)).findAny().orElse(null);
                List<GenomeRefSeq> allGenomeRef37Seqs = canvasDAOBeanService.getGenomeRefSeqDAO()
                        .findByGenomeRefIdAndSeqType(genomeRef37.getId(), "Chromosome");

                GenomeRef genomeRef38 = allGenomeRefs.stream().filter(a -> a.getId().equals(2)).findAny().orElse(null);
                List<GenomeRefSeq> allGenomeRef38Seqs = canvasDAOBeanService.getGenomeRefSeqDAO()
                        .findByGenomeRefIdAndSeqType(genomeRef38.getId(), "Chromosome");

                for (Triple<Integer, Integer, Integer> triple : filteredTriples) {

                    File latestVCF = FTPFactory.ncbiDownload(dbsnpDir, String.format("/snp/organisms/human_9606_b%s_GRCh%sp%s/VCF",
                            triple.getLeft(), triple.getMiddle(), triple.getRight()), "00-All.vcf.gz");

                    File latestVCFIndex = FTPFactory.ncbiDownload(dbsnpDir, String.format("/snp/organisms/human_9606_b%s_GRCh%sp%s/VCF",
                            triple.getLeft(), triple.getMiddle(), triple.getRight()), "00-All.vcf.gz.tbi");

                    if (!triple.getMiddle().equals(38)) {
                        logger.error("refseqVersion != 38");
                        continue;
                    }

                    List<File> fragments = fragmentVariantContexts(latestVCF, latestVCFIndex, dbsnpTmpDir);

                    logger.info("fragments.size(): {}", fragments.size());
                    Date dateUpdated = new Date();

                    for (File serializationFile : fragments) {

                        List<VariantContext> vcList = null;
                        try (FileInputStream fis = new FileInputStream(serializationFile);
                                GZIPInputStream gzipis = new GZIPInputStream(fis, Double.valueOf(Math.pow(2, 16)).intValue());
                                ObjectInputStream ois = new ObjectInputStream(gzipis)) {
                            vcList = (List<VariantContext>) ois.readObject();
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }

                        if (CollectionUtils.isNotEmpty(vcList)) {

                            ExecutorService es = Executors.newFixedThreadPool(3);

                            for (VariantContext variantContext : vcList) {

                                es.submit(() -> {

                                    try {
                                        GenomeRefSeq genomeRefSeq = allGenomeRef38Seqs.stream()
                                                .filter(a -> a.getContig().equals(variantContext.getContig())).findFirst().get();

                                        for (Allele altAllele : variantContext.getAlternateAlleles()) {

                                            LocatedVariant locatedVariant = LocatedVariantFactory.create(genomeRef38, genomeRefSeq,
                                                    variantContext, altAllele, allVariantTypes);

                                            List<LocatedVariant> foundLocatedVariants = canvasDAOBeanService.getLocatedVariantDAO()
                                                    .findByExample(locatedVariant);
                                            if (CollectionUtils.isNotEmpty(foundLocatedVariants)) {
                                                locatedVariant = foundLocatedVariants.get(0);
                                            } else {
                                                locatedVariant.setId(canvasDAOBeanService.getLocatedVariantDAO().save(locatedVariant));
                                            }

                                            logger.info(locatedVariant.toString());

                                            // first try to find CanonicalAllele by LocatedVariant
                                            CanonicalAllele canonicalAllele = null;

                                            List<CanonicalAllele> foundCanonicalAlleles = canvasDAOBeanService.getCanonicalAlleleDAO()
                                                    .findByLocatedVariantId(locatedVariant.getId());

                                            if (CollectionUtils.isNotEmpty(foundCanonicalAlleles)) {
                                                canonicalAllele = foundCanonicalAlleles.get(0);
                                            } else {
                                                canonicalAllele = new CanonicalAllele();
                                                canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                                                canonicalAllele.getLocatedVariants().add(locatedVariant);
                                                canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                                            }

                                            // does canonical allele have liftOver LocatedVariant?
                                            Optional<LocatedVariant> optionalLocatedVariant = canonicalAllele.getLocatedVariants().stream()
                                                    .filter(a -> a.getGenomeRef().getId().equals(genomeRef37.getId())).findAny();

                                            if (!optionalLocatedVariant.isPresent()) {

                                                LocatedVariant liftOverLocatedVariant = liftOver(locatedVariant, genomeRef37,
                                                        allGenomeRef37Seqs);

                                                if (liftOverLocatedVariant != null) {

                                                    if (locatedVariant.getVariantType().getId().equals("ins")) {
                                                        // could have had a deletion in ref
                                                        liftOverLocatedVariant.setEndPosition(liftOverLocatedVariant.getPosition() + 1);
                                                    }

                                                    List<LocatedVariant> foundLiftOverLocatedVariants = canvasDAOBeanService
                                                            .getLocatedVariantDAO().findByExample(liftOverLocatedVariant);
                                                    if (CollectionUtils.isNotEmpty(foundLiftOverLocatedVariants)) {
                                                        liftOverLocatedVariant = foundLocatedVariants.get(0);
                                                    } else {
                                                        liftOverLocatedVariant.setId(
                                                                canvasDAOBeanService.getLocatedVariantDAO().save(liftOverLocatedVariant));
                                                    }
                                                    logger.info("liftOver: {}", liftOverLocatedVariant.toString());

                                                    if (!canonicalAllele.getLocatedVariants().contains(liftOverLocatedVariant)) {
                                                        canonicalAllele.getLocatedVariants().add(liftOverLocatedVariant);
                                                        canvasDAOBeanService.getCanonicalAlleleDAO().save(canonicalAllele);
                                                    }

                                                }

                                            }

                                            // Integer rsId = Integer.valueOf(variantContext.getID().replaceFirst("rs", ""));
                                            // List<SNP> foundSNPs = canvasDAOBeanService.getSNPDAO().findByRSId(rsId);
                                            // SNP snp = null;
                                            // if (CollectionUtils.isNotEmpty(foundSNPs)) {
                                            // snp = foundSNPs.get(0);
                                            // } else {
                                            // snp = new SNP(rsId, latestBuildVersion.toString(), dateUpdated);
                                            // snp.setId(canvasDAOBeanService.getSNPDAO().save(snp));
                                            // }
                                            //
                                            // logger.info(snp.toString());
                                            //
                                            // snp.getCanonicalAlleles().add(canonicalAllele);
                                            // canvasDAOBeanService.getSNPDAO().save(snp);

                                        }
                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                    }

                                });

                            }

                            es.shutdown();
                            if (!es.awaitTermination(2L, TimeUnit.DAYS)) {
                                es.shutdownNow();
                            }

                        }

                        logger.info(String.format("%s/%s done", fragments.indexOf(serializationFile) + 1, fragments.size()));
                        serializationFile.delete();

                    }

                }

                UpdateDiagnosticResultVersionCallable callable = new UpdateDiagnosticResultVersionCallable(canvasDAOBeanService);
                callable.setNote(String.format("Persisted latest dbSNP: %s", latestBuildVersion.toString()));
                Executors.newSingleThreadExecutor().submit(callable).get();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        });
        return null;
    }

    private LocatedVariant liftOver(LocatedVariant locatedVariant, GenomeRef genomeRef, List<GenomeRefSeq> all37GenomeRefSeqs)
            throws PrimerException {
        LocatedVariant ret = null;
        try {
            File chainFile = new File(String.format("%s/liftOver", System.getProperty("karaf.data")), "hg38ToHg19.over.chain.gz");
            LiftOver liftOver = new LiftOver(chainFile);
            Interval interval = new Interval(String.format("chr%s", locatedVariant.getGenomeRefSeq().getContig()),
                    locatedVariant.getPosition(), locatedVariant.getEndPosition());
            Interval loInterval = liftOver.liftOver(interval);
            if (loInterval != null) {

                Optional<GenomeRefSeq> optionalGenomeRefSeq = all37GenomeRefSeqs.stream()
                        .filter(a -> a.getContig().equals(locatedVariant.getGenomeRefSeq().getContig())).findAny();

                if (!optionalGenomeRefSeq.isPresent()) {
                    throw new PrimerException("GenomeRefSeq not found");
                }

                if (interval.length() != loInterval.length()) {
                    return null;
                }

                GenomeRefSeq liftOverGenomeRefSeq = optionalGenomeRefSeq.get();
                logger.info(liftOverGenomeRefSeq.toString());

                GeReSe4jBuild gereseq4jMgr = GeReSe4jBuild_37_3.getInstance();
                String referenceSequence = gereseq4jMgr.getRegion(liftOverGenomeRefSeq.getId(),
                        Range.between(loInterval.getStart(), loInterval.getEnd()), true);
                if (StringUtils.isNotEmpty(referenceSequence) && locatedVariant.getRef().equals(referenceSequence)) {
                    ret = new LocatedVariant(genomeRef, liftOverGenomeRefSeq, loInterval.getStart(), loInterval.getEnd(),
                            locatedVariant.getVariantType(), locatedVariant.getRef(), locatedVariant.getSeq());
                }

            }
        } catch (Exception e) {
            throw new PrimerException(e);
        }
        return ret;
    }

    private List<File> fragmentVariantContexts(File latestVCF, File latestVCFIndex, File dbsnpTmpDir) throws IOException {
        List<File> fragmentedVCFiles = new ArrayList<>();
        List<VariantContext> variantContextList = new ArrayList<>();

        try (VCFFileReader vcfFileReader = new VCFFileReader(latestVCF, latestVCFIndex)) {

            for (VariantContext variantContext : vcfFileReader) {
                variantContextList.add(variantContext);
                if ((variantContextList.size() % 100000) == 0) {
                    File f = new File(dbsnpTmpDir, String.format("%s", UUID.randomUUID().toString()));
                    fragmentedVCFiles.add(f);
                    try (FileOutputStream fos = new FileOutputStream(f);
                            GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                            ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
                        oos.writeObject(variantContextList);
                    }
                    variantContextList.clear();
                }
            }
        }

        File f = new File(dbsnpTmpDir, String.format("%s", UUID.randomUUID().toString()));
        fragmentedVCFiles.add(f);

        try (FileOutputStream fos = new FileOutputStream(f);
                GZIPOutputStream gzipos = new GZIPOutputStream(fos, Double.valueOf(Math.pow(2, 14)).intValue());
                ObjectOutputStream oos = new ObjectOutputStream(gzipos)) {
            oos.writeObject(variantContextList);
        }
        variantContextList.clear();

        return fragmentedVCFiles;
    }

}
