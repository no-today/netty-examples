package netty.examples.commons.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author no-today
 * @date 2022/05/25 15:42
 */
@Slf4j
public class RemotingCommandEncoder extends MessageToByteEncoder<RemotingCommand> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemotingCommand aRemotingCommand, ByteBuf out) throws Exception {
        aRemotingCommand.encode(out);

        log.debug("Encoder: {}", aRemotingCommand);
    }
}
