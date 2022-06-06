package netty.examples.remoting;

import io.netty.channel.Channel;
import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.remoting.exception.RemotingConnectException;
import netty.examples.remoting.exception.RemotingSendRequestException;
import netty.examples.remoting.exception.RemotingTimeoutException;
import netty.examples.remoting.exception.RemotingTooMuchRequestException;

import java.util.concurrent.ExecutorService;

/**
 * @author no-today
 * @date 2022/05/30 16:35
 */
public interface RemotingServer extends RemotingLaunch {

    /**
     * 注册请求处理器
     *
     * @param requestCode 请求编码
     * @param processor   处理器
     * @param executor    处理器执行线程池
     */
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * 注册默认请求处理器
     * <p>
     * 当根据请求码匹配不到处理器时, 会使用该处理器
     *
     * @param processor 默认处理器
     * @param executor  默认处理器执行线程池
     */
    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * 同步执行指令
     *
     * @param channel       通道
     * @param request       指令数据
     * @param timeoutMillis 超时时间
     * @return 响应指令
     */
    RemotingCommand invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步执行指令
     *
     * @param channel        通道
     * @param request        指令数据
     * @param timeoutMillis  超时时间
     * @param invokeCallback 响应回调
     */
    void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException;

    /**
     * 单向执行指令(写成功即完成)
     *
     * @param channel       通道
     * @param request       指令数据
     * @param timeoutMillis 超时时间
     */
    void invokeOneway(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException;
}
