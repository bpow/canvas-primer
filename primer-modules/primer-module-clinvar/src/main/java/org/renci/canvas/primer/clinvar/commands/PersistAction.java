package org.renci.canvas.primer.clinvar.commands;

import java.util.concurrent.Executors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.BundleContext;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "clinvar", name = "persist", description = "Persist ClinVar data")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    @Reference
    private BundleContext bundleContext;

    public PersistAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");

        Executors.newSingleThreadExecutor().submit(new PersistUsingSequenceLocation(canvasDAOBeanService));
        // Executors.newSingleThreadExecutor().submit(new PersistUsingHGVS(canvasDAOBeanService, bundleContext));

        return null;
    }

}
