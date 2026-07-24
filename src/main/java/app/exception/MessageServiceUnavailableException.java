package app.exception;

public class MessageServiceUnavailableException extends RuntimeException {

    public MessageServiceUnavailableException(Throwable cause) {
        super(cause);
    }
}
