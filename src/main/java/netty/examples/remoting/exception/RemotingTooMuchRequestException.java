package netty.examples.remoting.exception;

/**
 * @author no-today
 * @date 2022/05/31 09:18
 */
public class RemotingTooMuchRequestException extends RemotingException {

    public RemotingTooMuchRequestException(String message) {
        super(message);
    }
}
