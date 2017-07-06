package org.renci.canvas.primer.commons;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinbin.model.DiagnosticResultVersion;
import org.renci.canvas.dao.clinvar.model.ClinVarVersion;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDiagnosticResultVersionCallable implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDiagnosticResultVersionCallable.class);

    private String note;

    private CANVASDAOBeanService canvasDAOBeanService;

    public UpdateDiagnosticResultVersionCallable(CANVASDAOBeanService canvasDAOBeanService) {
        super();
        this.canvasDAOBeanService = canvasDAOBeanService;
    }

    @Override
    public Void call() throws Exception {
        logger.debug("ENTERING call()");

        Executors.newSingleThreadExecutor().execute(() -> {

            try {
                ExecutorService es = Executors.newFixedThreadPool(3);

                Future<Integer> latestHGMDVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getHGMDLocatedVariantDAO().findLatestVersion();
                });

                Future<Integer> latestGen1000SNPVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getOneKGenomesSNPPopulationMaxFrequencyDAO().findLatestVersion();
                });

                Future<Integer> latestGen1000IndelVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getOneKGenomesIndelMaxFrequencyDAO().findLatestVersion();
                });

                Future<String> latestDbSNPVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getSNPDAO().findLatestVersion();
                });

                Future<ClinVarVersion> latestClinVarVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getClinVarVersionDAO().findLatestVersion();
                });

                Future<Integer> latestDiagnosticBinGroupVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getDiagnosticGeneGroupVersionDAO().findMaxDiagnosticBinGroupVersion();
                });

                Future<String> latestGnomADVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getGnomADMaxVariantFrequencyDAO().findLatestVersion();
                });

                Future<String> latestRefSeqVersionFuture = es.submit(() -> {
                    return canvasDAOBeanService.getTranscriptRefSeqVersionDAO().findLatestVersion();
                });

                Future<GenomeRef> latestGenomeRefFuture = es.submit(() -> {
                    List<GenomeRef> allGenomeRefs = canvasDAOBeanService.getGenomeRefDAO().findAll();
                    allGenomeRefs.sort((a, b) -> b.getId().compareTo(a.getId()));
                    return allGenomeRefs.get(0);
                });

                es.shutdown();
                if (!es.awaitTermination(10L, TimeUnit.MINUTES)) {
                    es.shutdownNow();
                }

                Integer latestHGMDVersion = latestHGMDVersionFuture.get();
                Integer latestGen1000SNPVersion = latestGen1000SNPVersionFuture.get();
                Integer latestGen1000IndelVersion = latestGen1000IndelVersionFuture.get();
                String latestDbSNPVersion = latestDbSNPVersionFuture.get();
                ClinVarVersion latestClinVarVersion = latestClinVarVersionFuture.get();
                Integer diagnosticBinGroupVersion = latestDiagnosticBinGroupVersionFuture.get();
                String latestGnomADVersion = latestGnomADVersionFuture.get();
                String latestRefSeqVersion = latestRefSeqVersionFuture.get();
                GenomeRef genomeRef = latestGenomeRefFuture.get();

                BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
                Bundle bundle = bundleContext.getBundle();
                String algorithmVersion = bundle.getVersion().toString();

                DiagnosticResultVersion diagnosticResultVersion = new DiagnosticResultVersion();

                diagnosticResultVersion.setAlgorithmVersion(algorithmVersion);
                diagnosticResultVersion.setClinvarVersion(latestClinVarVersion);
                diagnosticResultVersion.setDiagnosticBinGroupVersion(diagnosticBinGroupVersion);
                diagnosticResultVersion.setDbsnpVersion(latestDbSNPVersion);
                diagnosticResultVersion.setGen1000IndelVersion(latestGen1000IndelVersion);
                diagnosticResultVersion.setGen1000SnpVersion(latestGen1000SNPVersion);
                diagnosticResultVersion.setGenomeRef(genomeRef);
                diagnosticResultVersion.setGnomadVersion(latestGnomADVersion);
                diagnosticResultVersion.setHgmdVersion(latestHGMDVersion);
                diagnosticResultVersion.setNote(note);
                diagnosticResultVersion.setRefseqVersion(latestRefSeqVersion);

                // these need to be set from the plugin
                diagnosticResultVersion.setVcfLoaderName("");
                diagnosticResultVersion.setVcfLoaderVersion("");

                diagnosticResultVersion.setId(canvasDAOBeanService.getDiagnosticResultVersionDAO().save(diagnosticResultVersion));
                logger.info(diagnosticResultVersion.toString());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        });

        return null;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}
