package org.renci.canvas.primer.dao.jpa;

import javax.inject.Singleton;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.renci.canvas.primer.dao.StatusDAO;
import org.renci.canvas.primer.dao.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@OsgiServiceProvider(classes = { StatusDAO.class })
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.SUPPORTS)
@Singleton
public class StatusDAOImpl extends BaseDAOImpl<Status, Long> implements StatusDAO {

    private static final Logger logger = LoggerFactory.getLogger(StatusDAOImpl.class);

    public StatusDAOImpl() {
        super();
    }

    @Override
    public Class<Status> getPersistentClass() {
        return Status.class;
    }

}
