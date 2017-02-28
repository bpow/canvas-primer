package org.renci.canvas.primer.genenames.model.search;

import java.util.List;

public class HGNCSearchResponse {

    private Integer numFound;

    private Integer start;

    private Integer maxScore;

    private List<HGNCSearchResponseDoc> docs;

    public HGNCSearchResponse() {
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

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public List<HGNCSearchResponseDoc> getDocs() {
        return docs;
    }

    public void setDocs(List<HGNCSearchResponseDoc> docs) {
        this.docs = docs;
    }

    @Override
    public String toString() {
        return String.format("HGNCSearchResponse [numFound=%s, start=%s, maxScore=%s]", numFound, start, maxScore);
    }

}
