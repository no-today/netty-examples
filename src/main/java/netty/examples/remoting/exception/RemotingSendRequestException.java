package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/31 08:51
 */
public class RemotingSendRequestException extends RemotingException {

    public RemotingSendRequestException(String message) {
        super(message);
    }

    public RemotingSendRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
