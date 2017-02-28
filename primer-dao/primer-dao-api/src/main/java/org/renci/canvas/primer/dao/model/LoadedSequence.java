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
@Table(schema = "primer", name = "loaded_sequence")
public class LoadedSequence implements Persistable {

    private static final long serialVersionUID = 6476706496805781665L;

    @Id()
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loaded_sequence_id_seq")
    @SequenceGenerator(name = "loaded_sequence_id_seq", schema = "primer", sequenceName = "loaded_sequence_id_seq", allocationSize = 1, initialValue = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "genomic_reference_id")
    private Integer genomicReferenceId;

    @Column(name = "versioned_accession")
    private String versionedAccession;

    public LoadedSequence() {
        super();
    }

    public LoadedSequence(Integer genomicReferenceId, String versionedAccession) {
        super();
        this.genomicReferenceId = genomicReferenceId;
        this.versionedAccession = versionedAccession;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getGenomicReferenceId() {
        return genomicReferenceId;
    }

    public void setGenomicReferenceId(Integer genomicReferenceId) {
        this.genomicReferenceId = genomicReferenceId;
    }

    public String getVersionedAccession() {
        return versionedAccession;
    }

    public void setVersionedAccession(String versionedAccession) {
        this.versionedAccession = versionedAccession;
    }

    @Override
    public String toString() {
        return String.format("LoadedSequence [id=%s, genomicReferenceId=%s, versionedAccession=%s]", id, genomicReferenceId,
                versionedAccession);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((genomicReferenceId == null) ? 0 : genomicReferenceId.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((versionedAccession == null) ? 0 : versionedAccession.hashCode());
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
        LoadedSequence other = (LoadedSequence) obj;
        if (genomicReferenceId == null) {
            if (other.genomicReferenceId != null)
                return false;
        } else if (!genomicReferenceId.equals(other.genomicReferenceId))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (versionedAccession == null) {
            if (other.versionedAccession != null)
                return false;
        } else if (!versionedAccession.equals(other.versionedAccession))
            return false;
        return true;
    }

}
