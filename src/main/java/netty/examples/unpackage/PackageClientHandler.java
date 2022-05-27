package netty.examples.unpackage;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author no-today
 * @date 2022/05/25 17:10
 */
@Slf4j
@ChannelHandler.Sharable
public class PackageClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("Received: {}", msg.toString());
    }
}
