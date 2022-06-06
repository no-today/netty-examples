package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/31 08:50
 */
public class RemotingConnectException extends RemotingException {

    public RemotingConnectException(String message) {
        super(message);
    }

    public RemotingConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
