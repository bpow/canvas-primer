package org.renci.canvas.primer.commons;

public class PrimerException extends Exception {

    private static final long serialVersionUID = 1559994228502243354L;

    public PrimerException() {
        super();
    }

    public PrimerException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrimerException(String message) {
        super(message);
    }

    public PrimerException(Throwable cause) {
        super(cause);
    }

}
