package org.renci.canvas.primer.clinvar.commands;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.primer.commons.FTPFactory;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "clinvar", name = "persist", description = "Persist ClinVar data")
@Service
public class PersistAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(PersistAction.class);

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

        String date = FastDateFormat.getInstance("yyyy-MM").format(new Date());

        Path outputPath = Paths.get(System.getProperty("karaf.data"), "tmp", "clinvar");
        File outputDir = outputPath.toFile();
        outputDir.mkdirs();

        File clinvarXmlFile = FTPFactory.ncbiDownload(outputDir, "/pub/clinvar/xml", String.format("ClinVarFullRelease_%s.xml.gz", date));
        List<String> schemaFileList = FTPFactory.ncbiListRemoteFiles("/pub/clinvar/xml", "clinvar_public_", ".xsd");

        Pattern p = Pattern.compile("clinvar_public_(?<v1>//d+)\\.(?<v2>//d+)\\.xsd");
        List<Pair<Integer, Integer>> versionPairList = new ArrayList<>();
        for (String schemaFile : schemaFileList) {
            Matcher m = p.matcher(schemaFile);
            if (m.find()) {
                String v1 = m.group("v1");
                String v2 = m.group("v2");
                versionPairList.add(Pair.of(Integer.valueOf(v1), Integer.valueOf(v2)));
            }
        }

        versionPairList.sort((a, b) -> {
            int ret = b.getLeft().compareTo(a.getLeft());
            if (ret == 0) {
                ret = b.getRight().compareTo(a.getRight());
            }
            return ret;
        });

        File clinvarXSDFile = FTPFactory.ncbiDownload(outputDir, "/pub/clinvar/xsd_public",
                String.format("clinvar_public_%d.%d.xsd", versionPairList.get(0).getLeft(), versionPairList.get(0).getRight()));

        return null;
    }

}
