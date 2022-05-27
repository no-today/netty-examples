package netty.examples.unpackage;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import netty.examples.commons.protocol.RemotingCommand;

/**
 * @author no-today
 * @date 2022/05/25 17:05
 */
@Slf4j
@ChannelHandler.Sharable
public class PackageServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        log.info("Received: {}", msg);
    }
}
