package org.renci.canvas.primer.dao;

public interface PrimerDAOBeanService {

    public MappingsDAO getMappingsDAO();

    public void setMappingsDAO(MappingsDAO mappingsDAO);

    public MappingsDatabaseLoadedDAO getMappingsDatabaseLoadedDAO();

    public void setMappingsDatabaseLoadedDAO(MappingsDatabaseLoadedDAO mappingsDatabaseLoadedDAO);

    public StatusDAO getStatusDAO();

    public void setStatusDAO(StatusDAO statusDAO);

    public DatabaseLoadedDAO getDatabaseLoadedDAO();

    public void setDatabaseLoadedDAO(DatabaseLoadedDAO databaseLoadedDAO);

    public LoadedMetadataDAO getLoadedMetadataDAO();

    public void setLoadedMetadataDAO(LoadedMetadataDAO loadedMetadataDAO);

    public LoadedSequenceDAO getLoadedSequenceDAO();

    public void setLoadedSequenceDAO(LoadedSequenceDAO loadedSequenceDAO);

}