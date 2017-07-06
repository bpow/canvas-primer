package org.renci.canvas.primer.genes.commands;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.annotation.model.AnnotationGene;
import org.renci.canvas.dao.clinbin.model.DX;
import org.renci.canvas.dao.clinbin.model.DiagnosticGene;
import org.renci.canvas.dao.clinbin.model.DiagnosticGeneGroupVersion;
import org.renci.canvas.dao.clinbin.model.DiagnosticGeneGroupVersionPK;
import org.renci.canvas.primer.commons.UpdateDiagnosticResultVersionCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "gene-lists", name = "add-diagnostic-gene-list", description = "")
@Service
public class AddDiagnosticGeneListAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(AddDiagnosticGeneListAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Option(name = "--dxName", description = "DX Name", required = true, multiValued = false)
    private String dxName;

    @Option(name = "--geneNameInheritanceTierMapFile", description = "GeneName, Inheritance, Tier map file", required = true, multiValued = false)
    private String geneNameInheritanceTierMapFile;

    public AddDiagnosticGeneListAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        logger.info(dxName);

        File geneNameInheritanceTierMap = new File(geneNameInheritanceTierMapFile);
        if (!geneNameInheritanceTierMap.exists()) {
            logger.error("geneNameInheritanceTierMapFile does not exist: {}", geneNameInheritanceTierMapFile);
            return null;
        }

        Executors.newSingleThreadExecutor().submit(() -> {

            try {

                if (dxName.contains("\"")) {
                    dxName = dxName.replaceAll("\"", "");
                }

                DX dx = null;

                List<DX> foundDXs = canvasDAOBeanService.getDXDAO().findByName(dxName);
                if (CollectionUtils.isEmpty(foundDXs)) {
                    dx = new DX(dxName);
                    dx.setId(canvasDAOBeanService.getDXDAO().save(dx));
                } else {
                    dx = foundDXs.get(0);
                }
                logger.info(dx.toString());

                Integer diagnosticListVersion = canvasDAOBeanService.getDiagnosticGeneDAO().findMaxDiagnosticListVersionByDxId(dx.getId());
                if (diagnosticListVersion == null) {
                    diagnosticListVersion = 0;
                }
                logger.info("diagnosticListVersion: {}", diagnosticListVersion);
                ++diagnosticListVersion;

                Integer maxDiagnosticBinGroupVersion = canvasDAOBeanService.getDiagnosticGeneGroupVersionDAO()
                        .findMaxDiagnosticBinGroupVersion();
                if (maxDiagnosticBinGroupVersion == null) {
                    maxDiagnosticBinGroupVersion = 0;
                }
                logger.info("maxDiagnosticBinGroupVersion: {}", maxDiagnosticBinGroupVersion);

                List<DiagnosticGeneGroupVersion> diagnosticGeneGroupVersions = canvasDAOBeanService.getDiagnosticGeneGroupVersionDAO()
                        .findByDBinGroupVersion(maxDiagnosticBinGroupVersion);

                ++maxDiagnosticBinGroupVersion;

                final DX finalDX = dx;
                final Integer finalDiagnosticListVersion = diagnosticListVersion;

                Files.lines(geneNameInheritanceTierMap.toPath()).forEach(a -> {

                    try {
                        logger.info(a);

                        String[] dataSplit = a.split(",");
                        String gene = dataSplit[0].trim();
                        List<AnnotationGene> genes = canvasDAOBeanService.getAnnotationGeneDAO().findByName(gene);
                        if (CollectionUtils.isEmpty(genes)) {
                            logger.error("Gene not found: %s\n", gene);
                            return;
                        }

                        AnnotationGene annotationGene = genes.get(0);
                        logger.info(annotationGene.toString());

                        String inheritance = dataSplit[1];
                        String tier = dataSplit[2];

                        DiagnosticGene diagnosticGene = new DiagnosticGene(annotationGene, finalDiagnosticListVersion, finalDX, tier,
                                inheritance);

                        List<DiagnosticGene> foundDiagnosticGenes = canvasDAOBeanService.getDiagnosticGeneDAO()
                                .findByExample(diagnosticGene);
                        logger.info("foundDiagnosticGenes.size(): {}", foundDiagnosticGenes.size());
                        if (CollectionUtils.isEmpty(foundDiagnosticGenes)) {
                            diagnosticGene.setId(canvasDAOBeanService.getDiagnosticGeneDAO().save(diagnosticGene));
                        } else {
                            diagnosticGene = foundDiagnosticGenes.get(0);
                        }
                        logger.info(diagnosticGene.toString());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }

                });

                for (DiagnosticGeneGroupVersion diagnosticGeneGroupVersion : diagnosticGeneGroupVersions) {

                    DiagnosticGeneGroupVersionPK diagnosticGeneGroupVersionPK = new DiagnosticGeneGroupVersionPK(
                            maxDiagnosticBinGroupVersion, diagnosticGeneGroupVersion.getDx().getId(),
                            diagnosticGeneGroupVersion.getId().getDiagnosticListVersion());
                    DiagnosticGeneGroupVersion newDiagnosticGeneGroupVersion = new DiagnosticGeneGroupVersion(diagnosticGeneGroupVersionPK);
                    newDiagnosticGeneGroupVersion.setDx(diagnosticGeneGroupVersion.getDx());
                    newDiagnosticGeneGroupVersion
                            .setId(canvasDAOBeanService.getDiagnosticGeneGroupVersionDAO().save(newDiagnosticGeneGroupVersion));
                    logger.info(newDiagnosticGeneGroupVersion.toString());
                }

                DiagnosticGeneGroupVersionPK diagnosticGeneGroupVersionPK = new DiagnosticGeneGroupVersionPK(maxDiagnosticBinGroupVersion,
                        dx.getId(), diagnosticListVersion);
                DiagnosticGeneGroupVersion newDiagnosticGeneGroupVersion = new DiagnosticGeneGroupVersion(diagnosticGeneGroupVersionPK);
                newDiagnosticGeneGroupVersion.setDx(dx);
                newDiagnosticGeneGroupVersion
                        .setId(canvasDAOBeanService.getDiagnosticGeneGroupVersionDAO().save(newDiagnosticGeneGroupVersion));
                logger.info(newDiagnosticGeneGroupVersion.toString());

                UpdateDiagnosticResultVersionCallable callable = new UpdateDiagnosticResultVersionCallable(canvasDAOBeanService);
                callable.setNote(String.format("Adding/Updating Diagnostic Gene List: %s", dx.toString()));
                Executors.newSingleThreadExecutor().submit(callable).get();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        });

        return null;
    }

    public String getDxName() {
        return dxName;
    }

    public void setDxName(String dxName) {
        this.dxName = dxName;
    }

    public String getGeneNameInheritanceTierMapFile() {
        return geneNameInheritanceTierMapFile;
    }

    public void setGeneNameInheritanceTierMapFile(String geneNameInheritanceTierMapFile) {
        this.geneNameInheritanceTierMapFile = geneNameInheritanceTierMapFile;
    }

}
