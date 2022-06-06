package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/31 08:50
 */
public class RemotingTimeoutException extends RemotingException {

    public RemotingTimeoutException(long timeoutMillis, Throwable cause) {
        super("Timeout waiting for channel response, timeout millis is " + timeoutMillis, cause);
    }
}
