package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/30 19:57
 */
public class RemotingException extends Exception {

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
