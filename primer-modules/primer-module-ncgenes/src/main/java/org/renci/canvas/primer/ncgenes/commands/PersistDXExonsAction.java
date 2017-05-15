package org.renci.canvas.primer.ncgenes.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.annotation.model.AnnotationGene;
import org.renci.canvas.dao.annotation.model.AnnotationGeneExternalId;
import org.renci.canvas.dao.clinbin.model.DXExons;
import org.renci.canvas.dao.clinbin.model.DiagnosticGene;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.renci.canvas.dao.refseq.model.RefSeqGene;
import org.renci.canvas.dao.refseq.model.TranscriptMaps;
import org.renci.canvas.dao.refseq.model.TranscriptMapsExons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ncgenes", name = "persist-dx-exons", description = "")
@Service
public class PersistDXExonsAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistDXExonsAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    public PersistDXExonsAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        List<DiagnosticResultVersion> allDiagnosticResultVersions = canvasDAOBeanService.getDiagnosticResultVersionDAO().findAll();
        if (CollectionUtils.isNotEmpty(allDiagnosticResultVersions)) {

            allDiagnosticResultVersions.sort((a, b) -> b.getId().compareTo(a.getId()));

            DiagnosticResultVersion latestDiagnosticResultVersion = allDiagnosticResultVersions.get(0);

            List<DiagnosticGene> diagnosticGenes = canvasDAOBeanService.getDiagnosticGeneDAO()
                    .findByGroupVersionAndExternalNamespaceAndVersion(latestDiagnosticResultVersion.getDbinGroupVersion(), "refseq",
                            latestDiagnosticResultVersion.getRefseqVersion().toString());

            if (CollectionUtils.isNotEmpty(diagnosticGenes)) {

                for (DiagnosticGene diagnosticGene : diagnosticGenes) {
                    try {
                        AnnotationGene annotationGene = diagnosticGene.getGene();
                        List<AnnotationGeneExternalId> externals = canvasDAOBeanService.getAnnotationGeneExternalIdDAO()
                                .findByAnnotationGeneId(annotationGene.getId());
                        if (CollectionUtils.isNotEmpty(externals)) {
                            List<AnnotationGeneExternalId> filteredExternals = externals.stream()
                                    .filter(b -> "refseq".equals(b.getId().getNamespace()) && latestDiagnosticResultVersion
                                            .getRefseqVersion().toString().equals(b.getId().getNamespaceVer()))
                                    .collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(filteredExternals)) {
                                RefSeqGene refseqGene = canvasDAOBeanService.getRefSeqGeneDAO()
                                        .findById(filteredExternals.get(0).getId().getExternalId());
                                List<TranscriptMaps> transcriptMapsList = canvasDAOBeanService.getTranscriptMapsDAO()
                                        .findByGeneIdAndGenomeRefId(refseqGene.getId(),
                                                latestDiagnosticResultVersion.getGenomeRef().getId());

                                for (TranscriptMaps transcriptMaps : transcriptMapsList) {

                                    for (TranscriptMapsExons exons : transcriptMaps.getExons()) {

                                        DXExons dxExons = new DXExons(latestDiagnosticResultVersion.getId(), diagnosticGene,
                                                transcriptMaps.getTranscript().getId(), exons.getId().getExonNum(),
                                                transcriptMaps.getGenomeRefSeq().getId(), exons.getContigStart(), exons.getContigEnd(),
                                                transcriptMaps.getMapCount());

                                        List<DXExons> exonList = canvasDAOBeanService.getDXExonsDAO().findByExample(dxExons);
                                        if (CollectionUtils.isEmpty(exonList)) {
                                            dxExons.setId(canvasDAOBeanService.getDXExonsDAO().save(dxExons));
                                        }

                                    }

                                }

                            }
                        }
                    } catch (CANVASDAOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        return null;
    }

}
