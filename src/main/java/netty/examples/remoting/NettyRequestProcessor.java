package netty.examples.remoting;

import io.netty.channel.ChannelHandlerContext;
import netty.examples.commons.protocol.RemotingCommand;

/**
 * @author no-today
 * @date 2022/05/29 16:58
 */
public interface NettyRequestProcessor {

    /**
     * 拒绝请求
     */
    boolean rejectRequest();

    /**
     * 处理请求
     *
     * @param ctx     channel handler context
     * @param request request
     * @return response
     */
    RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception;
}
