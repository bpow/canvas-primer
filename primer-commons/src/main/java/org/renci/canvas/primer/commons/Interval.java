package org.renci.canvas.primer.commons;

public class Interval implements Comparable<Interval> {

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

}
