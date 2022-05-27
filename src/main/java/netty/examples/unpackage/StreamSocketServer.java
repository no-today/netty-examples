package netty.examples.unpackage;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import netty.examples.SocketServer;
import netty.examples.commons.protocol.RemotingCommandDecoder;
import netty.examples.commons.protocol.RemotingCommandEncoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author no-today
 * @date 2022/05/25 17:02
 */
@Slf4j
public class StreamSocketServer implements SocketServer {


    private final String host;
    private final int port;

    private final int bossLoopGroupThreads = 1;
    private final int workerLoopGroupThreads = 3;

    private NioEventLoopGroup boss;
    private NioEventLoopGroup worker;

    public StreamSocketServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() {
        boss = new NioEventLoopGroup(bossLoopGroupThreads);
        worker = new NioEventLoopGroup(workerLoopGroupThreads);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel channel) throws Exception {
                channel.pipeline().addLast(new RemotingCommandDecoder());
                channel.pipeline().addLast(new RemotingCommandEncoder());

                channel.pipeline().addLast(new PackageServerHandler());
            }
        });

        ChannelFuture channelFuture;
        if ("0.0.0.0".equals(host) || "localhost".equals(host)) {
            channelFuture = bootstrap.bind(port);
        } else {
            try {
                channelFuture = bootstrap.bind(new InetSocketAddress(InetAddress.getByName(host), port));
            } catch (UnknownHostException e) {
                channelFuture = bootstrap.bind(host, port);
                e.printStackTrace();
            }
        }

        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        log.info("Server started on {}:{}", host, port);
    }

    @Override
    public void shutdown() {
        if (boss != null) boss.shutdownGracefully();
        if (worker != null) worker.shutdownGracefully();
    }

    public static void main(String[] args) {
        new StreamSocketServer("localhost", 9999).start();
    }
}
