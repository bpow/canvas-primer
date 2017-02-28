package org.renci.canvas.primer.genenames.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HGNCSearchResponseHeader {

    private Integer status;

    @JsonProperty(value = "QTime")
    private Integer qtime;

    public HGNCSearchResponseHeader() {
        super();
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getQtime() {
        return qtime;
    }

    public void setQtime(Integer qTime) {
        this.qtime = qTime;
    }

    @Override
    public String toString() {
        return String.format("HGNCSearchResponseHeader [status=%s, qtime=%s]", status, qtime);
    }

}
