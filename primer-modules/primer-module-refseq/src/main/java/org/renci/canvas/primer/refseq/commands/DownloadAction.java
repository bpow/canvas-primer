package org.renci.canvas.primer.refseq.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "refseq", name = "download", description = "Download RefSeq files")
@Service
public class DownloadAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private PrimerDAOBeanService annotationDAOBeanService;

    public DownloadAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        return null;
    }

}
