package netty.examples.pingpong;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author no-today
 * @date 2022/05/27 16:02
 */
public class PingPongIntegrationTest {

    @Test
    void test() throws Exception {
        String host = "localhost";
        int port = 8888;

        PongSocketServer server = new PongSocketServer(host, port);
        PingSocketClient client = new PingSocketClient(host, port);
        ReconnectSocketClient client_r = new ReconnectSocketClient(host, port);

        server.start();
        client.connect();
        client_r.connect();

        TimeUnit.SECONDS.sleep(15);
        client.disconnect();
        client_r.disconnect();
        server.shutdown();
    }
}
