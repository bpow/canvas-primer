package org.renci.canvas.primer.dao.jpa;

import javax.inject.Singleton;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.renci.canvas.primer.dao.LoadedSequenceDAO;
import org.renci.canvas.primer.dao.model.LoadedSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@OsgiServiceProvider(classes = { LoadedSequenceDAO.class })
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.SUPPORTS)
@Singleton
public class LoadedSequenceDAOImpl extends BaseDAOImpl<LoadedSequence, Long> implements LoadedSequenceDAO {

    private static final Logger logger = LoggerFactory.getLogger(LoadedSequenceDAOImpl.class);

    public LoadedSequenceDAOImpl() {
        super();
    }

    @Override
    public Class<LoadedSequence> getPersistentClass() {
        return LoadedSequence.class;
    }

}
