package org.renci.canvas.primer.genenames.model.fetch;

public class HGNCFetch {

    private HGNCFetchResponse response;

    private HGNCFetchResponseHeader responseHeader;

    public HGNCFetch() {
        super();
    }

    public HGNCFetchResponse getResponse() {
        return response;
    }

    public void setResponse(HGNCFetchResponse response) {
        this.response = response;
    }

    public HGNCFetchResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(HGNCFetchResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

}
