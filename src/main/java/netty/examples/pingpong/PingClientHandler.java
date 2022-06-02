package netty.examples.pingpong;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author no-today
 * @date 2022/05/14 20:59
 */
@Slf4j
@ChannelHandler.Sharable
public class PingClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final AtomicInteger requestId = new AtomicInteger(1);
    private final ConcurrentMap<Integer, Long> responseTable = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel Active");

        ping(ctx, -1);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        long receiveNanoTime = getLosslessNanoTime();
        String[] split = msg.toString(CharsetUtil.UTF_8).split(":");

        Long sendNanoTime = responseTable.getOrDefault(Integer.parseInt(split[1]), Long.MAX_VALUE);
        long rtt = TimeUnit.NANOSECONDS.toMillis(Math.max(-1, receiveNanoTime - sendNanoTime));

        log.info("Channel Read: {}, RTT: {}", split[0], rtt);

        TimeUnit.SECONDS.sleep(3);

        ping(ctx, rtt);
    }

    private long getLosslessNanoTime() {
        long start = System.nanoTime();
        long now = System.nanoTime();
        long end = System.nanoTime();
        return now + (end - start);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("User Event Triggered: {}", evt);
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught: ", cause);
        ctx.close();
    }

    private void ping(ChannelHandlerContext ctx, long rtt) {
        int requestId = PingClientHandler.requestId.getAndIncrement();
        responseTable.put(requestId, getLosslessNanoTime());

        ctx.writeAndFlush(Unpooled.copiedBuffer("PING:" + requestId + ":" + rtt, CharsetUtil.UTF_8));
    }
}
