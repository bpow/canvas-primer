package org.renci.canvas.primer.commons;

import java.io.Serializable;

public class FastaSequence implements Serializable {

    private static final long serialVersionUID = -834069786209841550L;

    private String header;

    private StringBuilder sequence;

    public FastaSequence(String header) {
        super();
        this.header = header;
        this.sequence = new StringBuilder();
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public StringBuilder getSequence() {
        return sequence;
    }

    public void setSequence(StringBuilder sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return String.format("FastaSequence [header=%s]", header);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((header == null) ? 0 : header.hashCode());
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
        FastaSequence other = (FastaSequence) obj;
        if (header == null) {
            if (other.header != null)
                return false;
        } else if (!header.equals(other.header))
            return false;
        return true;
    }

}
