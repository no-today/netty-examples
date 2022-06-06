package netty.examples.remoting.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.commons.protocol.RemotingCommandDecoder;
import netty.examples.commons.protocol.RemotingCommandEncoder;
import netty.examples.remoting.InvokeCallback;
import netty.examples.remoting.NettyRequestProcessor;
import netty.examples.remoting.RemotingServer;
import netty.examples.remoting.exception.RemotingConnectException;
import netty.examples.remoting.exception.RemotingSendRequestException;
import netty.examples.remoting.exception.RemotingTimeoutException;
import netty.examples.remoting.exception.RemotingTooMuchRequestException;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author no-today
 * @date 2022/06/06 10:50
 */
@Slf4j
public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {

    private final NettyServerConfig nettyServerConfig;

    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupBoss;
    private final EventLoopGroup eventLoopGroupWorker;

    private final ExecutorService publicExecutor;

    private Timer timer = new Timer("CleanExpiredRequests", true);

    public NettyRemotingServer(NettyServerConfig nettyServerConfig) {
        super(nettyServerConfig.getAsyncSemaphoreValue(), nettyServerConfig.getOnewaySemaphoreValue());
        this.nettyServerConfig = nettyServerConfig;

        this.serverBootstrap = new ServerBootstrap();
        this.eventLoopGroupBoss = new NioEventLoopGroup(nettyServerConfig.getBossThreads(), new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyServerNioBoss_%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.eventLoopGroupWorker = new NioEventLoopGroup(nettyServerConfig.getWorkerThreads(), new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyServerNioWorker_%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.publicExecutor = Executors.newFixedThreadPool(Math.max(4, nettyServerConfig.getCallbackExecutorThreads()), new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyServerPublicExecutor_%d", this.threadIndex.incrementAndGet()));
            }
        });
    }

    @Override
    public void launch() {
        ServerBootstrap childHandler = this.serverBootstrap.group(this.eventLoopGroupWorker, this.eventLoopGroupWorker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, this.nettyServerConfig.getSocketBacklog())
                .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new RemotingCommandDecoder(),
                                new RemotingCommandEncoder(),
                                new IdleStateHandler(0, 0, nettyServerConfig.getChannelMaxIdleTimeSeconds()),
                                new NettyServerHandler());
                    }
                });

        if (nettyServerConfig.getSocketSndBufSize() > 0) {
            log.info("server set SO_SNDBUF to {}", nettyServerConfig.getSocketSndBufSize());
            childHandler.childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getSocketSndBufSize());
        }
        if (nettyServerConfig.getSocketRcvBufSize() > 0) {
            log.info("server set SO_RCVBUF to {}", nettyServerConfig.getSocketRcvBufSize());
            childHandler.childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getSocketRcvBufSize());
        }
        if (nettyServerConfig.getWriteBufferLowWaterMark() > 0 && nettyServerConfig.getWriteBufferHighWaterMark() > 0) {
            log.info("server set netty WRITE_BUFFER_WATER_MARK to {},{}",
                    nettyServerConfig.getWriteBufferLowWaterMark(), nettyServerConfig.getWriteBufferHighWaterMark());
            childHandler.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                    nettyServerConfig.getWriteBufferLowWaterMark(), nettyServerConfig.getWriteBufferHighWaterMark()));
        }

        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            this.serverBootstrap.bind().sync();
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }

        this.timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    scanResponseTable();
                } catch (Throwable e) {
                    log.error("ScanResponseTable exception", e);
                }
            }
        }, 1000 * 3, 1000);
    }

    @Override
    public void shutdown() {
        this.timer.cancel();

        this.eventLoopGroupBoss.shutdownGracefully();
        this.eventLoopGroupWorker.shutdownGracefully();
        this.publicExecutor.shutdown();
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.publicExecutor;
    }

    @ChannelHandler.Sharable
    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        super.processorTable.put(requestCode, new Pair<>(processor, executor));
    }

    @Override
    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        super.defaultRequestProcessor = new Pair<>(processor, executor);
    }

    @Override
    public RemotingCommand invokeSync(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        return super.invokeSyncImpl(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException {
        super.invokeAsyncImpl(channel, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException {
        super.invokeOnewayImpl(channel, request, timeoutMillis);
    }
}
