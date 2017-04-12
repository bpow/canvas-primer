package org.renci.canvas.primer.gr.commands;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_37_3;
import org.renci.gerese4j.core.impl.GeReSe4jBuild_38_7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadAndSerializeCallable implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAndSerializeCallable.class);

    public DownloadAndSerializeCallable() {
        super();
    }

    @Override
    public Void call() throws Exception {
        logger.debug("ENTERING call()");

        List<GeReSe4jBuild> gerese4jBuilds = Arrays.asList(GeReSe4jBuild_37_3.getInstance(), GeReSe4jBuild_38_7.getInstance());

        for (GeReSe4jBuild gerese4jBuild : gerese4jBuilds) {
            gerese4jBuild.serialize();
        }

        return null;
    }

}