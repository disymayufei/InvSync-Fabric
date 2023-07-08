package cn.disy920.invsync.exception;

public class DatabaseCreateException extends RuntimeException {
    public DatabaseCreateException() {
        super();
    }

    public DatabaseCreateException(String msg) {
        super(msg);
    }

    public DatabaseCreateException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DatabaseCreateException(Throwable cause) {
        super(cause);
    }
}
