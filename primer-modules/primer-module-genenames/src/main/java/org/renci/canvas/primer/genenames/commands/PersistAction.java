package org.renci.canvas.primer.genenames.commands;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.annotation.model.AnnotationGene;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalId;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalIdPK;
import org.renci.canvas.dao.annotation.model.AnnotationGeneSynonym;
import org.renci.canvas.dao.annotation.model.AnnotationGeneSynonymPK;
import org.renci.canvas.dao.hgnc.model.HGNCGene;
import org.renci.canvas.dao.hgnc.model.HGNCStatusType;
import org.renci.canvas.dao.hgnc.model.LocusGroup;
import org.renci.canvas.dao.hgnc.model.LocusType;
import org.renci.canvas.primer.commons.FTPFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "genenames", name = "persist", description = "Persist HGNC gene names")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    private static final Pattern locationPattern = Pattern.compile("(?<chr>\\d+)[a-z]\\.?\\d+?");

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();
            try {

                Path outputPath = Paths.get(System.getProperty("karaf.data"), "genenames");
                File genenamesDir = outputPath.toFile();
                File genenamesTmpDir = new File(genenamesDir, "tmp");
                genenamesTmpDir.mkdirs();

                File hgncCompleteSetFile = FTPFactory.download(genenamesTmpDir, "ftp.ebi.ac.uk", "/pub/databases/genenames/new/tsv",
                        "hgnc_complete_set.txt");

                logger.info("initializing dictionary tables");
                Set<String> statusSet = new HashSet<>();
                Set<String> locusGroupSet = new HashSet<>();
                Set<String> locusTypeSet = new HashSet<>();

                try (Reader in = new FileReader(hgncCompleteSetFile)) {

                    Iterable<CSVRecord> records = CSVFormat.TDF.withFirstRecordAsHeader().parse(in);

                    for (CSVRecord record : records) {
                        String status = record.get("status");
                        if (StringUtils.isNotEmpty(status)) {
                            statusSet.add(status.trim());
                        }

                        String locusGroupValue = record.get("locus_group");
                        if (StringUtils.isNotEmpty(locusGroupValue)) {
                            locusGroupSet.add(locusGroupValue.trim());
                        }

                        String locusTypeValue = record.get("locus_type");
                        if (StringUtils.isNotEmpty(locusTypeValue)) {
                            locusTypeSet.add(locusTypeValue.trim());
                        }
                    }

                }

                statusSet.forEach(status -> {
                    try {
                        HGNCStatusType hgncStatusType = canvasDAOBeanService.getHGNCStatusTypeDAO().findById(status);
                        if (hgncStatusType == null) {
                            canvasDAOBeanService.getHGNCStatusTypeDAO().save(new HGNCStatusType(status));
                        }
                    } catch (CANVASDAOException e) {
                        logger.error(e.getMessage(), e);
                    }
                });

                locusGroupSet.forEach(locusGroupValue -> {
                    try {
                        LocusGroup locusGroup = canvasDAOBeanService.getLocusGroupDAO().findById(locusGroupValue);
                        if (locusGroup == null) {
                            canvasDAOBeanService.getLocusGroupDAO().save(new LocusGroup(locusGroupValue));
                        }
                    } catch (CANVASDAOException e) {
                        logger.error(e.getMessage(), e);
                    }
                });

                locusTypeSet.forEach(locusTypeValue -> {
                    try {
                        LocusType locusType = canvasDAOBeanService.getLocusTypeDAO().findById(locusTypeValue);
                        if (locusType == null) {
                            canvasDAOBeanService.getLocusTypeDAO().save(new LocusType(locusTypeValue));
                        }
                    } catch (CANVASDAOException e) {
                        logger.error(e.getMessage(), e);
                    }
                });

                final List<HGNCStatusType> statusTypeList = canvasDAOBeanService.getHGNCStatusTypeDAO().findAll();
                final List<LocusGroup> locusGroupList = canvasDAOBeanService.getLocusGroupDAO().findAll();
                final List<LocusType> locusTypeList = canvasDAOBeanService.getLocusTypeDAO().findAll();

                logger.info("persisting");
                try (Reader in = new FileReader(hgncCompleteSetFile)) {

                    Iterable<CSVRecord> records = CSVFormat.TDF.withFirstRecordAsHeader().parse(in);

                    ExecutorService es = Executors.newFixedThreadPool(4);

                    for (CSVRecord record : records) {

                        es.submit(() -> {

                            try {
                                String hgncId = record.get("hgnc_id");
                                String symbol = record.get("symbol");
                                String name = record.get("name");
                                String locusGroup = record.get("locus_group");
                                String locusType = record.get("locus_type");
                                String status = record.get("status");
                                String location = record.get("location");
                                String aliasSymbol = record.get("alias_symbol");
                                String prevSymbol = record.get("prev_symbol");
                                String dateModified = record.get("date_modified");

                                HGNCGene hgncGene = new HGNCGene(Integer.valueOf(hgncId.split(":")[1]), name, symbol);

                                hgncGene.setStatus(statusTypeList.stream().filter(a -> a.getId().equals(status)).findFirst().get());
                                hgncGene.setLocusGroup(locusGroupList.stream().filter(a -> a.getId().equals(locusGroup)).findFirst().get());
                                hgncGene.setLocusType(locusTypeList.stream().filter(a -> a.getId().equals(locusType)).findFirst().get());

                                hgncGene.setDateModified(new java.sql.Date(DateUtils.parseDate(dateModified, "yyyy-MM-dd").getTime()));
                                if (StringUtils.isNotEmpty(location)) {
                                    hgncGene.setChromosomeRegion(location);
                                    Matcher m = locationPattern.matcher(location);
                                    if (m.find()) {
                                        String chromosome = m.group("chr");
                                        hgncGene.setChromosome(chromosome);
                                    }
                                }
                                HGNCGene foundGene = canvasDAOBeanService.getHGNCGeneDAO().findById(hgncGene.getId());
                                if (foundGene == null) {
                                    canvasDAOBeanService.getHGNCGeneDAO().save(hgncGene);
                                }
                                logger.info(hgncGene.toString());

                                AnnotationGene annotationGene = null;

                                List<AnnotationGene> foundAnnotationGenes = canvasDAOBeanService.getAnnotationGeneDAO()
                                        .findByName(hgncGene.getSymbol());
                                if (CollectionUtils.isEmpty(foundAnnotationGenes)) {
                                    annotationGene = new AnnotationGene(hgncGene.getSymbol(), hgncGene.getName());
                                    annotationGene.setId(canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene));
                                } else {
                                    annotationGene = foundAnnotationGenes.get(0);
                                }
                                logger.debug(annotationGene.toString());

                                AnnotationGeneExternalIdPK annotationGeneExternalIdPK = new AnnotationGeneExternalIdPK(hgncGene.getId(),
                                        annotationGene.getId(), "HGNC");

                                AnnotationGeneExternalId annotationGeneExternalId = null;

                                AnnotationGeneExternalId foundAnnotationGeneExternalId = canvasDAOBeanService
                                        .getAnnotationGeneExternalIdDAO().findById(annotationGeneExternalIdPK);

                                if (foundAnnotationGeneExternalId == null) {

                                    annotationGeneExternalId = new AnnotationGeneExternalId(annotationGeneExternalIdPK);
                                    annotationGeneExternalId.setGene(annotationGene);
                                    canvasDAOBeanService.getAnnotationGeneExternalIdDAO().save(annotationGeneExternalId);

                                } else {
                                    annotationGeneExternalId = foundAnnotationGeneExternalId;
                                }
                                logger.debug(annotationGeneExternalId.toString());

                                List<AnnotationGeneExternalId> externalIds = canvasDAOBeanService.getAnnotationGeneExternalIdDAO()
                                        .findByAnnotationGeneId(annotationGene.getId());

                                if (annotationGene.getExternals() == null) {
                                    annotationGene.setExternals(new HashSet<>());
                                    annotationGene.getExternals().addAll(externalIds);
                                }

                                annotationGene.getExternals().add(annotationGeneExternalId);
                                canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);

                                List<AnnotationGeneSynonym> annotationGeneSynonyms = canvasDAOBeanService.getAnnotationGeneSynonymDAO()
                                        .findByGeneId(annotationGene.getId());

                                Set<AnnotationGeneSynonym> annotationGeneSynonymSet = new HashSet<>(annotationGeneSynonyms);

                                List<String> synonymList = new ArrayList<>();
                                if (StringUtils.isNotEmpty(prevSymbol)) {
                                    if (prevSymbol.contains("|")) {
                                        Arrays.asList(prevSymbol.split("|")).stream().forEach(synonymList::add);
                                    } else {
                                        synonymList.add(prevSymbol);
                                    }
                                }
                                if (StringUtils.isNotEmpty(aliasSymbol)) {
                                    if (aliasSymbol.contains("|")) {
                                        Arrays.asList(aliasSymbol.split("|")).stream().forEach(synonymList::add);
                                    } else {
                                        synonymList.add(aliasSymbol);
                                    }
                                }

                                if (CollectionUtils.isNotEmpty(synonymList)) {
                                    for (String synonym : synonymList) {
                                        AnnotationGeneSynonymPK annotationGeneSynonymPK = new AnnotationGeneSynonymPK(
                                                annotationGene.getId(), synonym);
                                        AnnotationGeneSynonym foundAnnotationGeneSynonym = canvasDAOBeanService
                                                .getAnnotationGeneSynonymDAO().findById(annotationGeneSynonymPK);
                                        if (foundAnnotationGeneSynonym == null) {
                                            AnnotationGeneSynonym annotationGeneSynonym = new AnnotationGeneSynonym(
                                                    annotationGeneSynonymPK);
                                            annotationGeneSynonym.setGene(annotationGene);
                                            annotationGeneSynonym
                                                    .setId(canvasDAOBeanService.getAnnotationGeneSynonymDAO().save(annotationGeneSynonym));
                                            logger.debug(annotationGeneSynonym.toString());
                                            annotationGeneSynonymSet.add(annotationGeneSynonym);
                                        } else {
                                            annotationGeneSynonymSet.add(foundAnnotationGeneSynonym);
                                        }

                                    }
                                }

                                annotationGene.setSynonyms(annotationGeneSynonymSet);
                                canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }

                        });

                    }
                    es.shutdown();
                    // should only take about 20 minutes
                    if (!es.awaitTermination(1L, TimeUnit.HOURS)) {
                        es.shutdownNow();
                    }
                }

                hgncCompleteSetFile.delete();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            long end = System.currentTimeMillis();
            logger.info("duration = {}", String.format("%s seconds", (end - start) / 1000D));

        });

        return null;
    }

}
