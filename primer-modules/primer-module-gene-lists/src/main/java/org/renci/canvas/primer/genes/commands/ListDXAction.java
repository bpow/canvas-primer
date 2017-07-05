package org.renci.canvas.primer.genes.commands;

import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.clinbin.model.DX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "gene-lists", name = "list-dx", description = "")
@Service
public class ListDXAction implements Action {

    private static final Logger logger = LoggerFactory.getLogger(ListDXAction.class);

    @Reference
    private CANVASDAOBeanService canvasDAOBeanService;

    public ListDXAction() {
        super();
    }

    @Override
    public Object execute() throws Exception {
        logger.debug("ENTERING execute()");
        List<DX> allDXs = canvasDAOBeanService.getDXDAO().findAll();

        if (CollectionUtils.isNotEmpty(allDXs)) {

            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);
            String format = "%1$-12s %2$s%n";
            formatter.format(format, "ID", "Name");

            for (DX dx : allDXs) {
                formatter.format(format, dx.getId(), dx.getName());
                formatter.flush();
            }
            System.out.print(formatter.toString());
            formatter.close();
        } else {
            System.out.print("No DX instances found");
        }

        return null;
    }

}
