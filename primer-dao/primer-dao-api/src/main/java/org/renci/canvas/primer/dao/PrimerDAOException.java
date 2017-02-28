package org.renci.canvas.primer.dao;

public class PrimerDAOException extends Exception {

    private static final long serialVersionUID = 5033504425787801829L;

    public PrimerDAOException() {
        super();
    }

    public PrimerDAOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PrimerDAOException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrimerDAOException(String message) {
        super(message);
    }

    public PrimerDAOException(Throwable cause) {
        super(cause);
    }

}
