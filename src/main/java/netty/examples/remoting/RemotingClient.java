package netty.examples.remoting;

import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.remoting.exception.RemotingConnectException;
import netty.examples.remoting.exception.RemotingTooMuchRequestException;
import netty.examples.remoting.exception.RemotingSendRequestException;
import netty.examples.remoting.exception.RemotingTimeoutException;

/**
 * @author no-today
 * @date 2022/05/30 16:34
 */
public interface RemotingClient extends RemotingLaunch {

    /**
     * 同步调用
     *
     * @param request       请求指令
     * @param timeoutMillis 超时时间
     * @return 响应指令
     */
    RemotingCommand invokeSync(final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步调用
     *
     * @param request        请求指令
     * @param timeoutMillis  超时时间
     * @param invokeCallback 响应回调
     */
    void invokeAsync(final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException;

    /**
     * 单向调用(发送消息，但不需要响应)
     *
     * @param request       请求指令
     * @param timeoutMillis 超时时间
     */
    void invokeOneway(final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException;
}
