package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/31 16:09
 */
public class RemotingRejectRequestException extends RemotingException {

    public RemotingRejectRequestException(String message) {
        super(message);
    }

    public RemotingRejectRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
