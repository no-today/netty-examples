package netty.examples.commons.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author no-today
 * @date 2022/05/25 16:12
 */
@Slf4j
public class RemotingCommandDecoder extends LengthFieldBasedFrameDecoder {

    public static final int MAX_FRAME_LENGTH = 4194304; // as 512kb

    public RemotingCommandDecoder() {
        super(MAX_FRAME_LENGTH, 20, 4, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) return null;
            return RemotingCommand.decode(frame);
        } catch (Exception e) {
            log.error("decode exception ", e);
            ctx.channel().close();
        } finally {
            if (null != frame) frame.release();
        }

        return null;
    }
}
