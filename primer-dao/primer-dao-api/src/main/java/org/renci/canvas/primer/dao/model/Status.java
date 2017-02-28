package org.renci.canvas.primer.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(schema = "primer", name = "status")
public class Status extends NamedEntity {

    private static final long serialVersionUID = -2523201759938204701L;

    @Column(name = "downloaded")
    private Boolean downloaded;

    @Column(name = "processed")
    private Boolean processed;

    public Status() {
        super();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(Boolean downloaded) {
        this.downloaded = downloaded;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return String.format("Status [id=%s, name=%s, version=%s, created=%s, downloaded=%s, processed=%s]", id, name, version, created,
                downloaded, processed);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((downloaded == null) ? 0 : downloaded.hashCode());
        result = prime * result + ((processed == null) ? 0 : processed.hashCode());
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
        Status other = (Status) obj;
        if (downloaded == null) {
            if (other.downloaded != null)
                return false;
        } else if (!downloaded.equals(other.downloaded))
            return false;
        if (processed == null) {
            if (other.processed != null)
                return false;
        } else if (!processed.equals(other.processed))
            return false;
        return true;
    }

}
