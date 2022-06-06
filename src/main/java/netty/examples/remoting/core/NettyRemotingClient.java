package netty.examples.remoting.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.commons.protocol.RemotingCommandDecoder;
import netty.examples.commons.protocol.RemotingCommandEncoder;
import netty.examples.remoting.InvokeCallback;
import netty.examples.remoting.RemotingClient;
import netty.examples.remoting.exception.RemotingConnectException;
import netty.examples.remoting.exception.RemotingSendRequestException;
import netty.examples.remoting.exception.RemotingTimeoutException;
import netty.examples.remoting.exception.RemotingTooMuchRequestException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author no-today
 * @date 2022/06/05 10:26
 */
@Slf4j
public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {

    private final NettyClientConfig nettyClientConfig;

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroupBoss;
    private final Timer timer = new Timer("CleanExpiredRequests", true);

    private final ExecutorService publicExecutor;

    private Channel channel;

    public NettyRemotingClient(NettyClientConfig nettyClientConfig) {
        super(nettyClientConfig.getAsyncSemaphoreValue(), nettyClientConfig.getOnewaySemaphoreValue());
        this.nettyClientConfig = nettyClientConfig;

        this.bootstrap = new Bootstrap();
        this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyClientNioBoss_%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.publicExecutor = Executors.newFixedThreadPool(Math.min(4, nettyClientConfig.getCallbackExecutorThreads()), new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyClientPublicExecutor_%d", this.threadIndex.incrementAndGet()));
            }
        });
    }

    @Override
    public void launch() {
        Bootstrap handler = this.bootstrap.group(this.eventLoopGroupBoss)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.nettyClientConfig.getConnectTimeoutMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                                new RemotingCommandDecoder(),
                                new RemotingCommandEncoder(),
                                new IdleStateHandler(0, 0, nettyClientConfig.getChannelMaxIdleTimeSeconds()),
                                new NettyClientHandler()
                        );
                    }
                });

        if (nettyClientConfig.getSocketSndBufSize() > 0) {
            log.info("client set SO_SNDBUF to {}", nettyClientConfig.getSocketSndBufSize());
            handler.option(ChannelOption.SO_SNDBUF, nettyClientConfig.getSocketSndBufSize());
        }
        if (nettyClientConfig.getSocketRcvBufSize() > 0) {
            log.info("client set SO_RCVBUF to {}", nettyClientConfig.getSocketRcvBufSize());
            handler.option(ChannelOption.SO_RCVBUF, nettyClientConfig.getSocketRcvBufSize());
        }
        if (nettyClientConfig.getWriteBufferLowWaterMark() > 0 && nettyClientConfig.getWriteBufferHighWaterMark() > 0) {
            log.info("client set netty WRITE_BUFFER_WATER_MARK to {},{}",
                    nettyClientConfig.getWriteBufferLowWaterMark(), nettyClientConfig.getWriteBufferHighWaterMark());
            handler.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                    nettyClientConfig.getWriteBufferLowWaterMark(), nettyClientConfig.getWriteBufferHighWaterMark()));
        }

        this.channel = createChannel();

        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    scanResponseTable();
                } catch (Exception e) {
                    log.error("ScanResponseTable exception", e);
                }
            }
        }, 1000 * 3, 1000);
    }

    private Channel createChannel() {
        try {
            Channel channel = this.bootstrap.connect(nettyClientConfig.getHost(), nettyClientConfig.getPort()).sync().channel();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            return channel;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel getChannel() {
        return this.channel;
    }

    @Override
    public void shutdown() {
        this.timer.cancel();
        if (this.channel != null) this.channel.close();
        this.eventLoopGroupBoss.shutdownGracefully();
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.publicExecutor;
    }

    @ChannelHandler.Sharable
    class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    @Override
    public RemotingCommand invokeSync(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        return super.invokeSyncImpl(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException {
        super.invokeAsyncImpl(channel, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException {
        super.invokeOnewayImpl(channel, request, timeoutMillis);
    }
}
