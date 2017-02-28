package org.renci.canvas.primer.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(schema = "primer", name = "dbloaded")
public class DatabaseLoaded extends NamedEntity {

    private static final long serialVersionUID = 2785024783529864773L;

    @Column(name = "loaded_shard")
    private String loadedShard;

    public DatabaseLoaded() {
        super();
    }

    public String getLoadedShard() {
        return loadedShard;
    }

    public void setLoadedShard(String loadedShard) {
        this.loadedShard = loadedShard;
    }

    @Override
    public String toString() {
        return String.format("DatabaseLoaded [id=%s, name=%s, version=%s, created=%s, loadedShard=%s]", id, name, version, created,
                loadedShard);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((loadedShard == null) ? 0 : loadedShard.hashCode());
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
        DatabaseLoaded other = (DatabaseLoaded) obj;
        if (loadedShard == null) {
            if (other.loadedShard != null)
                return false;
        } else if (!loadedShard.equals(other.loadedShard))
            return false;
        return true;
    }

}
