package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/31 08:51
 */
public class RemotingCommandException extends RemotingException {

    public RemotingCommandException(String message) {
        super(message);
    }

    public RemotingCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
