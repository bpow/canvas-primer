package org.renci.canvas.primer.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(schema = "primer", name = "mappings")
public class Mappings extends NamedEntity {

    private static final long serialVersionUID = -2139468758477166670L;

    @Column(name = "reference_version")
    private String referenceVersion;

    @Column(name = "mapped")
    private Boolean mapped;

    public Mappings() {
        super();
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public Boolean getMapped() {
        return mapped;
    }

    public void setMapped(Boolean mapped) {
        this.mapped = mapped;
    }

    @Override
    public String toString() {
        return String.format("Mappings [id=%s, name=%s, version=%s, created=%s, referenceVersion=%s, mapped=%s]", id, name, version,
                created, referenceVersion, mapped);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((mapped == null) ? 0 : mapped.hashCode());
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
        Mappings other = (Mappings) obj;
        if (mapped == null) {
            if (other.mapped != null)
                return false;
        } else if (!mapped.equals(other.mapped))
            return false;
        if (referenceVersion == null) {
            if (other.referenceVersion != null)
                return false;
        } else if (!referenceVersion.equals(other.referenceVersion))
            return false;
        return true;
    }

}
