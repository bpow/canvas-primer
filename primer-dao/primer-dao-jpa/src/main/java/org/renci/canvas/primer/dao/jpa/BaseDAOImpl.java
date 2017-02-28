package org.renci.canvas.primer.dao.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.renci.canvas.primer.dao.BaseDAO;
import org.renci.canvas.primer.dao.Persistable;
import org.renci.canvas.primer.dao.PrimerDAOException;
import org.renci.canvas.primer.dao.StatusDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@OsgiServiceProvider(classes = { StatusDAO.class })
@org.springframework.transaction.annotation.Transactional
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.REQUIRED)
@Singleton
public abstract class BaseDAOImpl<T extends Persistable, ID extends Serializable> implements BaseDAO<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(BaseDAOImpl.class);

    @PersistenceContext(name = "primer", unitName = "primer")
    private EntityManager entityManager;

    public BaseDAOImpl() {
        super();
    }

    public abstract Class<T> getPersistentClass();

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @javax.transaction.Transactional(javax.transaction.Transactional.TxType.SUPPORTS)
    @Override
    public T findById(ID id) throws PrimerDAOException {
        logger.debug("ENTERING findById(T)");
        T ret = entityManager.find(getPersistentClass(), id);
        return ret;
    }

    @Override
    public synchronized void delete(T entity) throws PrimerDAOException {
        logger.debug("ENTERING delete(T)");
        T foundEntity = entityManager.find(getPersistentClass(), entity.getId());
        entityManager.remove(foundEntity);
    }

    @Override
    public synchronized void delete(List<T> entityList) throws PrimerDAOException {
        List<Long> idList = new ArrayList<Long>();
        for (T t : entityList) {
            idList.add(t.getId());
        }
        Query qDelete = entityManager.createQuery("delete from " + getPersistentClass().getSimpleName() + " a where a.id in (?1)");
        qDelete.setParameter(1, idList);
        qDelete.executeUpdate();
    }

    @Override
    public synchronized Long save(T entity) throws PrimerDAOException {
        logger.debug("ENTERING save(Entity)");
        if (entity == null) {
            logger.error("entity is null");
            return null;
        }
        if (!getEntityManager().contains(entity) && entity.getId() != null) {
            entity = getEntityManager().merge(entity);
        } else {
            getEntityManager().persist(entity);
        }
        return entity.getId();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

}
