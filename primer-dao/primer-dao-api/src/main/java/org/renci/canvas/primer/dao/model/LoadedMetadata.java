package org.renci.canvas.primer.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.renci.canvas.primer.dao.Persistable;

@Entity
@Table(schema = "primer", name = "loaded_metadata")
public class LoadedMetadata implements Persistable {

    private static final long serialVersionUID = -5827948579702856847L;

    @Id()
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loaded_metadata_id_seq")
    @SequenceGenerator(name = "loaded_metadata_id_seq", schema = "primer", sequenceName = "loaded_metadata_id_seq", allocationSize = 1, initialValue = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reference_version")
    private String referenceVersion;

    public LoadedMetadata() {
        super();
    }

    public LoadedMetadata(String referenceVersion) {
        super();
        this.referenceVersion = referenceVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    @Override
    public String toString() {
        return String.format("LoadedMetadata [id=%s, referenceVersion=%s]", id, referenceVersion);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((referenceVersion == null) ? 0 : referenceVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LoadedMetadata other = (LoadedMetadata) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (referenceVersion == null) {
            if (other.referenceVersion != null)
                return false;
        } else if (!referenceVersion.equals(other.referenceVersion))
            return false;
        return true;
    }

}
