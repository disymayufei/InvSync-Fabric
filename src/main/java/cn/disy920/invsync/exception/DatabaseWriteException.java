package cn.disy920.invsync.exception;

public class DatabaseWriteException extends RuntimeException {
    public DatabaseWriteException() {
        super();
    }

    public DatabaseWriteException(String msg) {
        super(msg);
    }

    public DatabaseWriteException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DatabaseWriteException(Throwable cause) {
        super(cause);
    }
}
