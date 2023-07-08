package cn.disy920.invsync.exception;

public class DatabaseReadException extends RuntimeException {
    public DatabaseReadException() {
        super();
    }

    public DatabaseReadException(String msg) {
        super(msg);
    }

    public DatabaseReadException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DatabaseReadException(Throwable cause) {
        super(cause);
    }
}

