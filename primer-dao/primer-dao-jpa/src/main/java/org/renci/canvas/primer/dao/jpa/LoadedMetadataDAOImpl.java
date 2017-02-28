package org.renci.canvas.primer.dao.jpa;

import javax.inject.Singleton;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.renci.canvas.primer.dao.LoadedMetadataDAO;
import org.renci.canvas.primer.dao.model.LoadedMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@OsgiServiceProvider(classes = { LoadedMetadataDAO.class })
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.SUPPORTS)
@Singleton
public class LoadedMetadataDAOImpl extends BaseDAOImpl<LoadedMetadata, Long> implements LoadedMetadataDAO {

    private static final Logger logger = LoggerFactory.getLogger(LoadedMetadataDAOImpl.class);

    public LoadedMetadataDAOImpl() {
        super();
    }

    @Override
    public Class<LoadedMetadata> getPersistentClass() {
        return LoadedMetadata.class;
    }

}
