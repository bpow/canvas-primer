package org.renci.canvas.primer.commons;

import java.io.Serializable;

public class Interval implements Serializable, Comparable<Interval> {

    private static final long serialVersionUID = 8964918174590607293L;

    private String accession;

    private Integer start;

    private Integer stop;

    private String geneName;

    private Integer index;

    private String transcript;

    private Integer dxId;

    public Interval() {
        super();
    }

    public Interval(String accession, Integer start, Integer stop, String geneName, Integer index, String transcript, Integer dxId) {
        super();
        this.accession = accession;
        this.start = start;
        this.stop = stop;
        this.geneName = geneName;
        this.index = index;
        this.transcript = transcript;
        this.dxId = dxId;
    }

    public Integer getDxId() {
        return dxId;
    }

    public void setDxId(Integer dxId) {
        this.dxId = dxId;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getStop() {
        return stop;
    }

    public void setStop(Integer stop) {
        this.stop = stop;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String toGATKIntervalList() {
        return String.format("%s:%s-%s", accession, start, stop);
    }

    public String toBedFormat() {
        return String.format("%s\t%s\t%s", accession, start, stop);
    }

    @Override
    public String toString() {
        return String.format("Interval [stop=%s, accession=%s, geneName=%s, start=%s, index=%s, transcript=%s]", stop, accession, geneName,
                start, index, transcript);
    }

    public String toStringRaw() {
        return String.format("%s\t%s\t%s\t%s\t%s\t%s", stop, accession, geneName, start, index, transcript);
    }

    @Override
    public int compareTo(Interval o) {
        int ret = this.accession.compareTo(o.getAccession());

        if (ret == 0) {
            ret = this.transcript.compareTo(o.getTranscript());
        }

        if (ret == 0) {
            ret = this.start.compareTo(o.getStart());
        }

        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accession == null) ? 0 : accession.hashCode());
        result = prime * result + ((dxId == null) ? 0 : dxId.hashCode());
        result = prime * result + ((geneName == null) ? 0 : geneName.hashCode());
        result = prime * result + ((index == null) ? 0 : index.hashCode());
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((stop == null) ? 0 : stop.hashCode());
        result = prime * result + ((transcript == null) ? 0 : transcript.hashCode());
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
        Interval other = (Interval) obj;
        if (accession == null) {
            if (other.accession != null)
                return false;
        } else if (!accession.equals(other.accession))
            return false;
        if (dxId == null) {
            if (other.dxId != null)
                return false;
        } else if (!dxId.equals(other.dxId))
            return false;
        if (geneName == null) {
            if (other.geneName != null)
                return false;
        } else if (!geneName.equals(other.geneName))
            return false;
        if (index == null) {
            if (other.index != null)
                return false;
        } else if (!index.equals(other.index))
            return false;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        if (stop == null) {
            if (other.stop != null)
                return false;
        } else if (!stop.equals(other.stop))
            return false;
        if (transcript == null) {
            if (other.transcript != null)
                return false;
        } else if (!transcript.equals(other.transcript))
            return false;
        return true;
    }

}
