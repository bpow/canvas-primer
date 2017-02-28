package org.renci.canvas.primer.dao;

import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PrimerDAOManager {

    private static PrimerDAOManager instance;

    private PrimerDAOBeanService daoBean;

    public static PrimerDAOManager getInstance() {
        if (instance == null) {
            instance = new PrimerDAOManager();
        }
        return instance;
    }

    private PrimerDAOManager() {
        super();
        // do not close ctx...will close entity manager
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:/dao-context.xml");
        this.daoBean = ctx.getBean(PrimerDAOBeanService.class);
    }

    public PrimerDAOBeanService getDAOBean() {
        return daoBean;
    }

    public void setDAOBean(PrimerDAOBeanService daoBean) {
        this.daoBean = daoBean;
    }
}
