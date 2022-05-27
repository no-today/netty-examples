package netty.examples.commons.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import netty.examples.SocketClient;

import java.util.concurrent.TimeUnit;

/**
 * @author no-today
 * @date 2022/05/25 09:48
 */
@Slf4j
@ChannelHandler.Sharable
public class ReconnectHandler extends ChannelInboundHandlerAdapter {

    private final SocketClient client;

    public ReconnectHandler(SocketClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().eventLoop().schedule(client::connect, 1, TimeUnit.SECONDS);
        ctx.fireChannelInactive();
    }
}
