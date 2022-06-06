package netty.examples.remoting.core;

import io.netty.channel.ChannelHandlerContext;
import netty.examples.commons.protocol.RemotingCommand;
import netty.examples.remoting.NettyRequestProcessor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author no-today
 * @date 2022/06/06 16:27
 */
class NettyRemotingServerTest {

    @Test
    void sysErrors() throws Exception {
        NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig());
        NettyRemotingClient client = new NettyRemotingClient(new NettyClientConfig());

        server.launch();
        client.launch();

        System.out.println(client.invokeSync(RemotingCommand.createRequestCommand(1024, "hello".getBytes(), Map.of("random", UUID.randomUUID().toString())), 500));
        client.invokeAsync(RemotingCommand.createRequestCommand(1024, "hello".getBytes(), Map.of("random", UUID.randomUUID().toString())), 500, response -> {
            System.out.println(response.getResponseCommand());
        });

        // 原封不动返回请求数据
        AtomicBoolean rejectRequest = new AtomicBoolean(true);
        server.registerDefaultProcessor(new NettyRequestProcessor() {
            @Override
            public boolean rejectRequest() {
                return rejectRequest.get();
            }

            @Override
            public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
                TimeUnit.MILLISECONDS.sleep(300);
                request.markResponseType();
                return request;
            }
        }, Executors.newFixedThreadPool(5));
        System.out.println(client.invokeSync(RemotingCommand.createRequestCommand(1024, "hello".getBytes(), Map.of("random", UUID.randomUUID().toString())), 500));

        rejectRequest.set(false);

        for (int i = 0; i < 10000; i++) {
            client.invokeAsync(RemotingCommand.createRequestCommand(1024, "hello".getBytes(), Map.of("random", UUID.randomUUID().toString())), 500, response -> {
                System.out.println(response.getResponseCommand());
            });
        }

        client.shutdown();
        server.shutdown();
    }
}