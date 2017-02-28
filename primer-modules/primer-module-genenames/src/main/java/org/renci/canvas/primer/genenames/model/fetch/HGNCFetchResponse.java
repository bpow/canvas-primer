package org.renci.canvas.primer.genenames.model.fetch;

import java.util.List;

public class HGNCFetchResponse {

    private Integer numFound;

    private Integer start;

    private List<HGNCFetchResponseDoc> docs;

    public HGNCFetchResponse() {
        super();
    }

    public Integer getNumFound() {
        return numFound;
    }

    public void setNumFound(Integer numFound) {
        this.numFound = numFound;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public List<HGNCFetchResponseDoc> getDocs() {
        return docs;
    }

    public void setDocs(List<HGNCFetchResponseDoc> docs) {
        this.docs = docs;
    }

    @Override
    public String toString() {
        return String.format("HGNCResponse [numFound=%s, start=%s]", numFound, start);
    }

}
