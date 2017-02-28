package org.renci.canvas.primer.dao.jpa;

import javax.inject.Singleton;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.renci.canvas.primer.dao.DatabaseLoadedDAO;
import org.renci.canvas.primer.dao.model.DatabaseLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@OsgiServiceProvider(classes = { DatabaseLoadedDAO.class })
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.SUPPORTS)
@Singleton
public class DatabaseLoadedDAOImpl extends BaseDAOImpl<DatabaseLoaded, Long> implements DatabaseLoadedDAO {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseLoadedDAOImpl.class);

    public DatabaseLoadedDAOImpl() {
        super();
    }

    @Override
    public Class<DatabaseLoaded> getPersistentClass() {
        return DatabaseLoaded.class;
    }

}
