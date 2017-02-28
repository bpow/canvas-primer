package org.renci.canvas.primer.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(schema = "primer", name = "mappings_dbloaded")
public class MappingsDatabaseLoaded extends NamedEntity {

    private static final long serialVersionUID = 953502612078963481L;

    @Column(name = "reference_version")
    private String referenceVersion;

    @Column(name = "loaded_shard")
    private String loadedShard;

    public MappingsDatabaseLoaded() {
        super();
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public String getLoadedShard() {
        return loadedShard;
    }

    public void setLoadedShard(String loadedShard) {
        this.loadedShard = loadedShard;
    }

    @Override
    public String toString() {
        return String.format("MappingsDatabaseLoaded [id=%s, name=%s, version=%s, created=%s, referenceVersion=%s, loadedShard=%s]", id,
                name, version, created, referenceVersion, loadedShard);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((loadedShard == null) ? 0 : loadedShard.hashCode());
        result = prime * result + ((referenceVersion == null) ? 0 : referenceVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MappingsDatabaseLoaded other = (MappingsDatabaseLoaded) obj;
        if (loadedShard == null) {
            if (other.loadedShard != null)
                return false;
        } else if (!loadedShard.equals(other.loadedShard))
            return false;
        if (referenceVersion == null) {
            if (other.referenceVersion != null)
                return false;
        } else if (!referenceVersion.equals(other.referenceVersion))
            return false;
        return true;
    }

}
