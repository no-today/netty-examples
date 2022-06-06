package netty.examples.remoting.core;

import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.remoting.InvokeCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 与请求匹配, 在未来某个时间点通知
 *
 * @author no-today
 * @date 2022/05/29 15:03
 */
public class ResponseFuture {

    private final long reqId;

    private final long timeoutMillis;
    private final long beginTimeMillis = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private volatile RemotingCommand responseCommand;
    private volatile boolean sendRequestOk = true;
    private volatile Throwable cause;
    private final InvokeCallback responseCallback;
    private final AtomicBoolean executeResponseCallbackOnlyOnce = new AtomicBoolean(false);
    private final SemaphoreReleaseOnlyOnce semaphoreReleaseOnlyOnce;

    public ResponseFuture(long reqId, long timeoutMillis, InvokeCallback responseCallback, SemaphoreReleaseOnlyOnce semaphoreReleaseOnlyOnce) {
        this.reqId = reqId;
        this.timeoutMillis = timeoutMillis;
        this.responseCallback = responseCallback;
        this.semaphoreReleaseOnlyOnce = semaphoreReleaseOnlyOnce;
    }

    public ResponseFuture(long reqId, long timeoutMillis) {
        this(reqId, timeoutMillis, null, null);
    }

    public void putResponse(final RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.countDownLatch.countDown();
    }

    public RemotingCommand waitResponse(final long timeoutMillis) throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCommand;
    }


    public long getReqId() {
        return reqId;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public long getBeginTimeMillis() {
        return beginTimeMillis;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setResponseCommand(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
    }

    public RemotingCommand getResponseCommand() {
        return responseCommand;
    }

    public void setSendRequestOk(boolean sendRequestOk) {
        this.sendRequestOk = sendRequestOk;
    }

    public boolean isSendRequestOk() {
        return sendRequestOk;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }

    public void executeCallback() {
        if (null != this.responseCallback && this.executeResponseCallbackOnlyOnce.compareAndSet(false, true)) {
            this.responseCallback.operationComplete(this);
        }
    }

    public void releaseSemaphore() {
        if (null != this.semaphoreReleaseOnlyOnce) {
            this.semaphoreReleaseOnlyOnce.release();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResponseFuture{");
        sb.append("reqId=").append(reqId);
        sb.append(", timeoutMillis=").append(timeoutMillis);
        sb.append(", beginTimeMillis=").append(beginTimeMillis);
        sb.append('}');
        return sb.toString();
    }
}
