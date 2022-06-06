package netty.examples.remoting.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.remoting.InvokeCallback;
import netty.examples.remoting.NettyRequestProcessor;
import netty.examples.remoting.exception.RemotingConnectException;
import netty.examples.remoting.exception.RemotingSendRequestException;
import netty.examples.remoting.exception.RemotingTimeoutException;
import netty.examples.remoting.exception.RemotingTooMuchRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author no-today
 * @date 2022/05/31 15:08
 */
public abstract class NettyRemotingAbstract {

    private static final Logger log = LoggerFactory.getLogger(NettyRemotingAbstract.class);

    /**
     * 异步命令信号量, 控制异步调用的并发数量, 从而保护系统内存
     */
    protected final Semaphore semaphoreAsync;

    /**
     * 单向命令信号量, 控制单向调用的并发数量, 从而保护系统内存
     */
    protected final Semaphore semaphoreOneway;

    /**
     * 缓存所有正在进行的请求(未响应)
     */
    protected final ConcurrentMap<Long /* request id */, ResponseFuture> responseTable = new ConcurrentHashMap<>(128);
    /**
     * 通过请求编码找到请求处理器
     */
    protected final Map<Integer /* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable = new HashMap<>(32);

    /**
     * 默认请求处理器
     */
    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    public NettyRemotingAbstract(int permitsAsync, int permitsOneway) {
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
    }

    public abstract ExecutorService getCallbackExecutor();

    public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        if (msg == null) return;
        switch (msg.getType()) {
            case REQUEST_COMMAND:
                processRequestCommand(ctx, msg);
                break;
            case RESPONSE_COMMAND:
                processResponseCommand(ctx, msg);
                break;
            default:
                break;
        }
    }

    public void processRequestCommand(ChannelHandlerContext ctx, RemotingCommand cmd) throws Exception {
        Pair<NettyRequestProcessor, ExecutorService> pair = this.processorTable.getOrDefault(cmd.getCode(), this.defaultRequestProcessor);
        if (pair == null) {
            ctx.writeAndFlush(RemotingCommand.createResponseCommand(cmd.getReqId(), RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, "[REQUEST_CODE_NOT_SUPPORTED] request code " + cmd.getCode() + " not supported."));
            return;
        }

        NettyRequestProcessor requestProcessor = pair.getObj1();
        if (requestProcessor.rejectRequest()) {
            ctx.writeAndFlush(RemotingCommand.createResponseCommand(cmd.getReqId(), RemotingSysResponseCode.COMMAND_NOT_AVAILABLE_NOW, "[COMMAND_UNAVAILABLE_NOW] this command is currently unavailable."));
            return;
        }

        Runnable task = () -> {
            try {
                ctx.writeAndFlush(requestProcessor.processRequest(ctx, cmd));
            } catch (Throwable e) {
                log.error("Cmd: {}", cmd);
                log.error("Exception", e);

                if (!cmd.isOneway()) {
                    ctx.writeAndFlush(RemotingCommand.createResponseCommand(cmd.getReqId(), RemotingSysResponseCode.SYSTEM_ERROR, e.getMessage()));
                }
            }
        };

        try {
            pair.getObj2().submit(task);
        } catch (RejectedExecutionException e) {
            // 10s print once log
            if (System.currentTimeMillis() % 10000 == 0) {
                log.warn("Too many requests and system thread pool busy, RejectedExecutionException");
            }

            if (!cmd.isOneway()) {
                ctx.writeAndFlush(RemotingCommand.createResponseCommand(cmd.getReqId(), RemotingSysResponseCode.SYSTEM_BUSY, "[OVERLOAD] system busy, try later."));
            }
        }
    }

    public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand cmd) throws Exception {
        long reqId = cmd.getReqId();
        ResponseFuture future = this.responseTable.remove(reqId);
        if (null != future) {
            future.putResponse(cmd);
            executionCallback(future);
            future.releaseSemaphore();
        } else {
            log.warn("Receive response command, but not matched any request, {}", cmd);
        }
    }

    /**
     * 扫描已经超时的请求, 并进行回调通知
     */
    public void scanResponseTable() {
        List<ResponseFuture> rfList = new LinkedList<>();
        Iterator<Map.Entry<Long, ResponseFuture>> it = responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ResponseFuture> next = it.next();
            ResponseFuture rep = next.getValue();

            if (rep.getBeginTimeMillis() + rep.getTimeoutMillis() <= System.currentTimeMillis()) {
                rep.releaseSemaphore();
                it.remove();
                rfList.add(rep);

                log.warn("Remove timeout request, {}", rep);
            }
        }

        for (ResponseFuture future : rfList) {
            executionCallback(future);
        }
    }

    protected RemotingCommand invokeSyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        if (null == channel || !channel.isActive()) {
            throw new RemotingConnectException("Channel unavailable, wait for reconnection");
        }

        long reqId = request.getReqId();
        ResponseFuture responseFuture = new ResponseFuture(reqId, timeoutMillis);
        this.responseTable.put(reqId, responseFuture);

        channel.writeAndFlush(request).addListener(future -> {
            if (future.isSuccess()) {
                responseFuture.setSendRequestOk(true);
            } else {
                responseFuture.setSendRequestOk(false);
                responseFuture.setCause(future.cause());
                responseFuture.putResponse(null);   // 触发闭锁
            }
        });

        try {
            RemotingCommand response = responseFuture.waitResponse(timeoutMillis);
            if (null == response) {
                if (responseFuture.isSendRequestOk()) {
                    throw new RemotingTimeoutException(timeoutMillis, responseFuture.getCause());
                } else {
                    throw new RemotingSendRequestException("Failed to send request to channel", responseFuture.getCause());
                }
            }

            return response;
        } finally {
            this.responseTable.remove(reqId);
        }
    }

    protected void invokeAsyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback callback) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTooMuchRequestException {
        if (null == channel || !channel.isActive()) {
            throw new RemotingConnectException("Channel unavailable, wait for reconnection");
        }

        if (!this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
            String info = String.format("InvokeAsync tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreOnewayValue: %d", timeoutMillis, this.semaphoreAsync.getQueueLength(), this.semaphoreAsync.availablePermits());
            throw new RemotingTooMuchRequestException(info);
        }

        long reqId = request.getReqId();
        ResponseFuture responseFuture = new ResponseFuture(reqId, timeoutMillis, callback, new SemaphoreReleaseOnlyOnce(this.semaphoreAsync));
        this.responseTable.put(reqId, responseFuture);

        try {
            channel.writeAndFlush(request).addListener(future -> {
                if (future.isSuccess()) {
                    responseFuture.setSendRequestOk(true);
                } else {
                    this.responseTable.remove(reqId);
                    responseFuture.setSendRequestOk(false);
                    responseFuture.setCause(future.cause());
                    responseFuture.putResponse(null);
                    executionCallback(responseFuture);
                }
            });
        } catch (Exception e) {
            String info = "Write a request command to channel failed";
            log.warn(info, e);
            throw new RemotingSendRequestException(info, e);
        }
    }

    protected void invokeOnewayImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTooMuchRequestException {
        if (null == channel || !channel.isActive()) {
            throw new RemotingConnectException("Channel unavailable, wait for reconnection");
        }

        if (!this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
            String info = String.format("InvokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreOnewayValue: %d", timeoutMillis, this.semaphoreOneway.getQueueLength(), this.semaphoreOneway.availablePermits());
            throw new RemotingTooMuchRequestException(info);
        }

        long reqId = request.getReqId();
        ResponseFuture responseFuture = new ResponseFuture(reqId, timeoutMillis, null, new SemaphoreReleaseOnlyOnce(this.semaphoreOneway));

        try {
            channel.writeAndFlush(request).addListener(future -> {
                responseFuture.releaseSemaphore();
                if (!future.isSuccess()) {
                    log.warn("Send a request command to channel failed");
                }
            });
        } catch (Exception e) {
            responseFuture.releaseSemaphore();
            String info = "Write a request command to channel failed";
            log.warn(info, e);
            throw new RemotingSendRequestException(info, e);
        }
    }

    protected void executionCallback(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = getCallbackExecutor();
        if (null != executor) {
            try {
                executor.submit(() -> {
                    try {
                        responseFuture.executeCallback();
                    } catch (Exception e) {
                        log.warn("Execute callback in executor exception, and callback throw", e);
                    } finally {
                        responseFuture.releaseSemaphore();
                    }
                });
            } catch (Exception e) {
                runInThisThread = true;
                log.warn("Execute callback in executor exception, maybe executor busy", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeCallback();
            } catch (Throwable e) {
                log.warn("ExecuteCallback Exception", e);
            } finally {
                responseFuture.releaseSemaphore();
            }
        }
    }
}
