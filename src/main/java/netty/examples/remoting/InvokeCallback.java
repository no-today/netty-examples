package netty.examples.remoting;

import netty.examples.remoting.core.ResponseFuture;

/**
 * @author no-today
 * @date 2022/05/30 19:25
 */
public interface InvokeCallback {

    void operationComplete(final ResponseFuture responseFuture);
}
