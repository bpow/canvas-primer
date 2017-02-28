package org.renci.canvas.primer.dao.jpa;

import org.renci.canvas.primer.dao.DatabaseLoadedDAO;
import org.renci.canvas.primer.dao.LoadedMetadataDAO;
import org.renci.canvas.primer.dao.LoadedSequenceDAO;
import org.renci.canvas.primer.dao.MappingsDAO;
import org.renci.canvas.primer.dao.MappingsDatabaseLoadedDAO;
import org.renci.canvas.primer.dao.PrimerDAOBeanService;
import org.renci.canvas.primer.dao.StatusDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrimerDAOBeanServiceImpl implements PrimerDAOBeanService {

    @Autowired
    private MappingsDAO mappingsDAO;

    @Autowired
    private MappingsDatabaseLoadedDAO mappingsDatabaseLoadedDAO;

    @Autowired
    private StatusDAO statusDAO;

    @Autowired
    private DatabaseLoadedDAO databaseLoadedDAO;

    @Autowired
    private LoadedMetadataDAO loadedMetadataDAO;

    @Autowired
    private LoadedSequenceDAO loadedSequenceDAO;

    public PrimerDAOBeanServiceImpl() {
        super();
    }

    public MappingsDAO getMappingsDAO() {
        return mappingsDAO;
    }

    public void setMappingsDAO(MappingsDAO mappingsDAO) {
        this.mappingsDAO = mappingsDAO;
    }

    public MappingsDatabaseLoadedDAO getMappingsDatabaseLoadedDAO() {
        return mappingsDatabaseLoadedDAO;
    }

    public void setMappingsDatabaseLoadedDAO(MappingsDatabaseLoadedDAO mappingsDatabaseLoadedDAO) {
        this.mappingsDatabaseLoadedDAO = mappingsDatabaseLoadedDAO;
    }

    public StatusDAO getStatusDAO() {
        return statusDAO;
    }

    public void setStatusDAO(StatusDAO statusDAO) {
        this.statusDAO = statusDAO;
    }

    public DatabaseLoadedDAO getDatabaseLoadedDAO() {
        return databaseLoadedDAO;
    }

    public void setDatabaseLoadedDAO(DatabaseLoadedDAO databaseLoadedDAO) {
        this.databaseLoadedDAO = databaseLoadedDAO;
    }

    public LoadedMetadataDAO getLoadedMetadataDAO() {
        return loadedMetadataDAO;
    }

    public void setLoadedMetadataDAO(LoadedMetadataDAO loadedMetadataDAO) {
        this.loadedMetadataDAO = loadedMetadataDAO;
    }

    public LoadedSequenceDAO getLoadedSequenceDAO() {
        return loadedSequenceDAO;
    }

    public void setLoadedSequenceDAO(LoadedSequenceDAO loadedSequenceDAO) {
        this.loadedSequenceDAO = loadedSequenceDAO;
    }

}
