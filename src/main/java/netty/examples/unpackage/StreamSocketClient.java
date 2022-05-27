package netty.examples.unpackage;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
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
import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.commons.protocol.RemotingCommandDecoder;
import netty.examples.commons.protocol.RemotingCommandEncoder;
import netty.examples.commons.retry.DefaultRetryPolicy;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author no-today
 * @date 2022/05/25 15:26
 */
@Slf4j
public class StreamSocketClient implements SocketClient {

    private final String host;
    private final int port;

    private Bootstrap bootstrap;
    private NioEventLoopGroup group;
    private Channel channel;

    private AtomicBoolean started = new AtomicBoolean();

    public StreamSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void setup() {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();

        SocketClient socketClient = this;
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG)).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ReconnectHandler(socketClient));

                ch.pipeline().addLast(new RemotingCommandDecoder());
                ch.pipeline().addLast(new RemotingCommandEncoder());

                ch.pipeline().addLast(new PackageClientHandler());
            }
        });
    }

    @Override
    public void connect() {
        setup();

        channel = DefaultRetryPolicy.retryCall(log, () -> bootstrap.connect(host, port).sync().channel());
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));

        log.info("Client connect on {}:{}", host, port);

        write();
    }

    private void write() {
        if (started.compareAndSet(false, true)) {
            new Thread(() -> {
                while (true) {
                    sleep(3000);

                    sticky();
                    half();
                }
            }).start();
        }
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // 半包
    private void half() {
        byte[] body = generateContent((int) (Math.random() * 1000)).getBytes();
        RemotingCommand command = new RemotingCommand((short) 1, (short) 0, 0, generateSequence(), body);

        channel.writeAndFlush(Unpooled.buffer(2).writeInt(RemotingCommand.MAGIC_CODE));
        channel.writeAndFlush(Unpooled.buffer(2).writeShort(command.getVersion()));
        channel.writeAndFlush(Unpooled.buffer(4).writeShort(command.getTerminal()));
        sleep(100);
        channel.writeAndFlush(Unpooled.buffer(4).writeInt(command.getType()));
        channel.writeAndFlush(Unpooled.buffer(4).writeLong(command.getSequence()));
        channel.writeAndFlush(Unpooled.buffer(4).writeInt(command.getLength()));
        sleep(100);
        channel.writeAndFlush(Unpooled.buffer(command.getLength()).writeBytes(command.getBody()));
    }

    private Long generateSequence() {
        return ((Double) Math.ceil((Math.random() * 1000000000000000000L))).longValue();
    }

    // 粘包
    private void sticky() {
        byte[] body1 = generateContent((int) (Math.random() * 1000)).getBytes();
        RemotingCommand command1 = new RemotingCommand((short) 2, (short) 1, 0, generateSequence(), body1);

        byte[] body2 = generateContent((int) (Math.random() * 1000)).getBytes();
        RemotingCommand command2 = new RemotingCommand((short) 2, (short) 2, 0, generateSequence(), body2);

        channel.write(command1);
        channel.writeAndFlush(command2);
    }

    private String generateContent(int count) {
        StringBuilder content = new StringBuilder(UUID.randomUUID().toString());
        while (--count > 0) content.append(UUID.randomUUID());
        return content.toString();
    }

    @Override
    public void disconnect() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) {
        new StreamSocketClient("localhost", 9999).connect();
    }
}
