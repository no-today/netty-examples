package netty.examples.remoting.core;

import netty.examples.remoting.InvokeCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author no-today
 * @date 2022/06/06 10:02
 */
public class ResponseFutureAsync extends ResponseFuture {

    /**
     * 执行响应回调
     */
    private final InvokeCallback responseCallback;

    /**
     * 确保响应回调只执行一次
     */
    private final AtomicBoolean executeResponseCallbackOnlyOnce = new AtomicBoolean(false);

    /**
     * 确保信号量只释放一次
     */
    private final SemaphoreReleaseOnlyOnce semaphoreReleaseOnlyOnce;

    public ResponseFutureAsync(long reqId, long timeoutMillis, InvokeCallback responseCallback, SemaphoreReleaseOnlyOnce semaphoreReleaseOnlyOnce) {
        super(reqId, timeoutMillis);
        this.responseCallback = responseCallback;
        this.semaphoreReleaseOnlyOnce = semaphoreReleaseOnlyOnce;
    }

    public void releaseSemaphore() {
        this.semaphoreReleaseOnlyOnce.release();
    }
}
