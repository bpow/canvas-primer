package org.renci.canvas.primer.genenames.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HGNCSearchResponseDoc {

    @JsonProperty("hgnc_id")
    private String hgncId;

    private String symbol;

    private Float score;

    public HGNCSearchResponseDoc() {
        super();
    }

    public String getHgncId() {
        return hgncId;
    }

    public void setHgncId(String hgncId) {
        this.hgncId = hgncId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return String.format("HGNCSearchResponseDoc [hgncId=%s, symbol=%s, score=%s]", hgncId, symbol, score);
    }

}
