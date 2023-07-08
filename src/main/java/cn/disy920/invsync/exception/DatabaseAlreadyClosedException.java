package cn.disy920.invsync.exception;

public class DatabaseAlreadyClosedException extends RuntimeException{
    public DatabaseAlreadyClosedException() {
        super();
    }

    public DatabaseAlreadyClosedException(String msg) {
        super(msg);
    }

    public DatabaseAlreadyClosedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DatabaseAlreadyClosedException(Throwable cause) {
        super(cause);
    }
}
