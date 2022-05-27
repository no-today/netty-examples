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
import netty.examples.commons.handler.ReconnectHandler;
import netty.examples.commons.retry.DefaultRetryPolicy;

/**
 * @author no-today
 * @date 2022/05/14 20:52
 */
@Slf4j
public class ReconnectSocketClient implements SocketClient {

    private final String host;
    private final int port;

    private Bootstrap bootstrap;
    private NioEventLoopGroup group;

    private Channel channel;

    public ReconnectSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setup() {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();

        SocketClient client = this;
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ReconnectHandler(client));
                    }
                });
    }

    @Override
    public void connect() {
        setup();

        channel = DefaultRetryPolicy.retryCall(log, () -> bootstrap.connect(host, port).sync().channel());
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));

        log.info("Client connect on {}:{}", host, port);
    }

    @Override
    public void disconnect() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        new ReconnectSocketClient("localhost", 8888).connect();
    }
}
