package org.renci.canvas.primer.ncgenes.commands;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinbin.model.DiagnosticGene;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ncgenes", name = "persist", description = "")
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

        List<DiagnosticResultVersion> allDiagnosticResultVersions = canvasDAOBeanService.getDiagnosticResultVersionDAO().findAll();
        if (CollectionUtils.isNotEmpty(allDiagnosticResultVersions)) {
            
            allDiagnosticResultVersions.sort((a, b) -> b.getId().compareTo(a.getId()));

            DiagnosticResultVersion latestDiagnosticResultVersion = allDiagnosticResultVersions.get(0);

            List<DiagnosticGene> diagnosticGenes = canvasDAOBeanService.getDiagnosticGeneDAO()
                    .findByGroupVersionAndExternalNamespaceAndVersion(latestDiagnosticResultVersion.getDbinGroupVersion(), "refseq",
                            latestDiagnosticResultVersion.getRefseqVersion().toString());

        }

        return null;
    }

}
