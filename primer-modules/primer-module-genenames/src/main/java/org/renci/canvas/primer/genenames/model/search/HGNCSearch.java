package org.renci.canvas.primer.genenames.model.search;

public class HGNCSearch {

    private HGNCSearchResponseHeader responseHeader;

    private HGNCSearchResponse response;

    public HGNCSearch() {
        super();
    }

    public HGNCSearchResponse getResponse() {
        return response;
    }

    public void setResponse(HGNCSearchResponse response) {
        this.response = response;
    }

    public HGNCSearchResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(HGNCSearchResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

}
