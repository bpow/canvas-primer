package org.renci.canvas.primer.dao.jpa;

import javax.inject.Singleton;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.renci.canvas.primer.dao.MappingsDAO;
import org.renci.canvas.primer.dao.model.Mappings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@OsgiServiceProvider(classes = { MappingsDAO.class })
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.SUPPORTS)
@Singleton
public class MappingsDAOImpl extends BaseDAOImpl<Mappings, Long> implements MappingsDAO {

    private static final Logger logger = LoggerFactory.getLogger(MappingsDAOImpl.class);

    public MappingsDAOImpl() {
        super();
    }

    @Override
    public Class<Mappings> getPersistentClass() {
        return Mappings.class;
    }

}
