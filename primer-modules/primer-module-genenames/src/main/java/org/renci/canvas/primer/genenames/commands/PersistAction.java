package org.renci.canvas.primer.genenames.commands;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
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
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.renci.canvas.primer.genenames.model.fetch.HGNCFetch;
import org.renci.canvas.primer.genenames.model.fetch.HGNCFetchResponse;
import org.renci.canvas.primer.genenames.model.fetch.HGNCFetchResponseDoc;
import org.renci.canvas.primer.genenames.model.search.HGNCSearch;
import org.renci.canvas.primer.genenames.model.search.HGNCSearchResponseDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Command(scope = "genenames", name = "persist", description = "Persist HGNC gene names")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    private static final Pattern locationPattern = Pattern.compile("(?<chr>\\d+)[a-z]\\.?\\d+?");

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private PrimerDAOBeanService annotationDAOBeanService;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().execute(() -> {
            long start = System.currentTimeMillis();
            try {

                final List<HGNCStatusType> statusTypeList = canvasDAOBeanService.getHGNCStatusTypeDAO().findAll();
                final List<LocusGroup> locusGroupList = canvasDAOBeanService.getLocusGroupDAO().findAll();
                final List<LocusType> locusTypeList = canvasDAOBeanService.getLocusTypeDAO().findAll();

                WebTarget target = ClientBuilder.newBuilder().newClient()
                        .target("http://rest.genenames.org/search/status:Approved+OR+status:%22Entry%20Withdrawn%22");
                Response response = target.request(MediaType.APPLICATION_JSON).get();

                ObjectMapper mapper = new ObjectMapper();
                HGNCSearch hgncSearch = mapper.readValue(response.readEntity(String.class), HGNCSearch.class);

                List<HGNCSearchResponseDoc> searchResponseDocs = hgncSearch.getResponse().getDocs();

                if (CollectionUtils.isNotEmpty(searchResponseDocs)) {
                    logger.info("searchResponseDocs.size(): {}", searchResponseDocs.size());

                    ExecutorService es = Executors.newFixedThreadPool(10);
                    for (HGNCSearchResponseDoc searchResponseDoc : searchResponseDocs) {
                        es.submit(() -> {

                            try {
                                WebTarget childTarget = ClientBuilder.newBuilder().newClient()
                                        .target(String.format("http://rest.genenames.org/fetch/symbol/%s", searchResponseDoc.getSymbol()));
                                Response fetchSymbolResponse = childTarget.request(MediaType.APPLICATION_JSON).get();

                                HGNCFetch hgncFetch = mapper.readValue(fetchSymbolResponse.readEntity(String.class), HGNCFetch.class);

                                HGNCFetchResponse fetchResponse = hgncFetch.getResponse();
                                List<HGNCFetchResponseDoc> fetchResponseDocs = fetchResponse.getDocs();

                                if (CollectionUtils.isNotEmpty(fetchResponseDocs)) {
                                    logger.debug("fetchResponseDocs.size(): {}", fetchResponseDocs.size());

                                    for (HGNCFetchResponseDoc fetchResponseDoc : fetchResponseDocs) {

                                        if (!statusTypeList.stream().filter(a -> a.getId().equals(fetchResponseDoc.getStatus())).findAny()
                                                .isPresent()) {
                                            HGNCStatusType statusType = new HGNCStatusType(fetchResponseDoc.getStatus());
                                            canvasDAOBeanService.getHGNCStatusTypeDAO().save(statusType);
                                            statusTypeList.add(statusType);
                                        }

                                        if (!locusGroupList.stream().filter(a -> a.getId().equals(fetchResponseDoc.getLocusGroup()))
                                                .findAny().isPresent()) {
                                            LocusGroup locusGroup = new LocusGroup(fetchResponseDoc.getLocusGroup());
                                            canvasDAOBeanService.getLocusGroupDAO().save(locusGroup);
                                            locusGroupList.add(locusGroup);
                                        }

                                        if (!locusTypeList.stream().filter(a -> a.getId().equals(fetchResponseDoc.getLocusType())).findAny()
                                                .isPresent()) {
                                            LocusType locusType = new LocusType(fetchResponseDoc.getLocusType());
                                            canvasDAOBeanService.getLocusTypeDAO().save(locusType);
                                            locusTypeList.add(locusType);
                                        }

                                        HGNCGene hgncGene = new HGNCGene(Integer.valueOf(fetchResponseDoc.getHgncId().split(":")[1]),
                                                fetchResponseDoc.getName(), fetchResponseDoc.getSymbol());
                                        hgncGene.setStatus(statusTypeList.stream()
                                                .filter(a -> a.getId().equals(fetchResponseDoc.getStatus())).findFirst().get());
                                        hgncGene.setLocusGroup(locusGroupList.stream()
                                                .filter(a -> a.getId().equals(fetchResponseDoc.getLocusGroup())).findFirst().get());
                                        hgncGene.setLocusType(locusTypeList.stream()
                                                .filter(a -> a.getId().equals(fetchResponseDoc.getLocusType())).findFirst().get());
                                        hgncGene.setDateModified(new java.sql.Date(DateUtils
                                                .parseDate(fetchResponseDoc.getDateModified(), "yyyy-MM-dd'T'HH:mm:ss'Z'").getTime()));
                                        if (StringUtils.isNotEmpty(fetchResponseDoc.getLocation())) {
                                            hgncGene.setChromosomeRegion(fetchResponseDoc.getLocation());
                                            Matcher m = locationPattern.matcher(fetchResponseDoc.getLocation());
                                            if (m.find()) {
                                                String chromosome = m.group("chr");
                                                hgncGene.setChromosome(chromosome);
                                            }
                                        }
                                        HGNCGene foundGene = canvasDAOBeanService.getHGNCGeneDAO().findById(hgncGene.getId());
                                        if (foundGene == null) {
                                            logger.info(hgncGene.toString());
                                            canvasDAOBeanService.getHGNCGeneDAO().save(hgncGene);
                                        }

                                        AnnotationGene annotationGene = new AnnotationGene(hgncGene.getSymbol(), hgncGene.getName());
                                        List<AnnotationGene> foundAnnotationGenes = canvasDAOBeanService.getAnnotationGeneDAO()
                                                .findByExample(annotationGene);
                                        if (CollectionUtils.isEmpty(foundAnnotationGenes)) {
                                            Integer geneId = canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);
                                            annotationGene.setId(geneId);
                                        } else {
                                            annotationGene = foundAnnotationGenes.get(0);
                                        }

                                        List<AnnotationGeneExternalId> annotationGeneExternalIds = canvasDAOBeanService
                                                .getAnnotationGeneExternalIdDAO().findByExternalIdAndNamespace(hgncGene.getId(), "HGNC");
                                        Set<AnnotationGeneExternalId> externalIdSet = new HashSet<>(annotationGeneExternalIds);

                                        AnnotationGeneExternalIdPK annotationGeneExternalIdPK = new AnnotationGeneExternalIdPK(
                                                hgncGene.getId(), annotationGene.getId(), "HGNC");

                                        if (CollectionUtils.isEmpty(annotationGeneExternalIds) || (CollectionUtils
                                                .isNotEmpty(annotationGeneExternalIds)
                                                && !annotationGeneExternalIds.stream()
                                                        .filter(a -> a.getId().equals(annotationGeneExternalIdPK)).findAny().isPresent())) {

                                            AnnotationGeneExternalId annotationGeneExternalId = new AnnotationGeneExternalId(
                                                    annotationGeneExternalIdPK);
                                            annotationGeneExternalId.setGene(annotationGene);
                                            canvasDAOBeanService.getAnnotationGeneExternalIdDAO().save(annotationGeneExternalId);
                                            externalIdSet.add(annotationGeneExternalId);

                                        }

                                        annotationGene.setExternals(externalIdSet);
                                        canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);

                                        List<AnnotationGeneSynonym> annotationGeneSynonyms = canvasDAOBeanService
                                                .getAnnotationGeneSynonymDAO().findByGeneId(annotationGene.getId());

                                        Set<AnnotationGeneSynonym> annotationGeneSynonymSet = new HashSet<>(annotationGeneSynonyms);

                                        List<String> synonymList = new ArrayList<>();
                                        if (CollectionUtils.isNotEmpty(fetchResponseDoc.getPreviousSymbols())) {
                                            synonymList.addAll(fetchResponseDoc.getPreviousSymbols());
                                        }
                                        if (CollectionUtils.isNotEmpty(fetchResponseDoc.getAliasSymbols())) {
                                            synonymList.addAll(fetchResponseDoc.getAliasSymbols());
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
                                                    annotationGeneSynonym.setId(
                                                            canvasDAOBeanService.getAnnotationGeneSynonymDAO().save(annotationGeneSynonym));
                                                    annotationGeneSynonymSet.add(annotationGeneSynonym);
                                                } else {
                                                    annotationGeneSynonymSet.add(foundAnnotationGeneSynonym);
                                                }
                                            }
                                        }

                                        annotationGene.setSynonyms(annotationGeneSynonymSet);
                                        canvasDAOBeanService.getAnnotationGeneDAO().save(annotationGene);

                                    }
                                }
                            } catch (NumberFormatException | IOException | CANVASDAOException | ParseException e) {
                                logger.error(e.getMessage(), e);
                            }

                        });
                    }
                    es.shutdown();
                    es.awaitTermination(1L, TimeUnit.DAYS);
                }

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            long end = System.currentTimeMillis();
            logger.info("duration = {}", String.format("%d seconds", (end - start) / 1000D));

        });

        return null;
    }

}
