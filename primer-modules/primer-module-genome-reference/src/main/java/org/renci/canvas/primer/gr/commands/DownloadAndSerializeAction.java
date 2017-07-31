package org.renci.canvas.primer.gr.commands;

import java.util.concurrent.Executors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "genome-reference", name = "serialize", description = "serialize")
@Service
public class DownloadAndSerializeAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAndSerializeAction.class);

    public DownloadAndSerializeAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");
        Executors.newSingleThreadExecutor().submit(new DownloadAndSerializeCallable());
        return null;
    }

}
