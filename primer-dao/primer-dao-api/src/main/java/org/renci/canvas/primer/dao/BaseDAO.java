package org.renci.canvas.primer.dao;

import java.io.Serializable;
import java.util.List;

public interface BaseDAO<T extends Persistable, ID extends Serializable> {

    public abstract Long save(T entity) throws PrimerDAOException;

    public abstract void delete(T entity) throws PrimerDAOException;

    public abstract void delete(List<T> idList) throws PrimerDAOException;

    public abstract T findById(ID id) throws PrimerDAOException;

}
