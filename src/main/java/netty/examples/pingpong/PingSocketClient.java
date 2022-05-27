package netty.examples.pingpong;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import netty.examples.SocketClient;

/**
 * @author no-today
 * @date 2022/05/14 20:52
 */
@Slf4j
public class PingSocketClient implements SocketClient {

    private final String host;
    private final int port;

    private Bootstrap bootstrap;
    private NioEventLoopGroup group;

    private Channel channel;

    public PingSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setup() {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new PingClientHandler());
                    }
                });
    }

    @Override
    public void connect() {
        setup();

        try {
            channel = bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));

        log.info("Client connect on {}:{}", host, port);
    }

    @Override
    public void disconnect() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        new PingSocketClient("localhost", 8888).connect();
    }
}
