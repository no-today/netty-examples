package netty.examples.unpackage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author no-today
 * @date 2022/05/27 16:06
 */
class UnPackageIntegrationTest {

    @Test
    void test() throws Exception {
        StreamSocketClient client = new StreamSocketClient("localhost", 9999);
        StreamSocketServer server = new StreamSocketServer("localhost", 9999);

        server.start();
        client.connect();

        TimeUnit.SECONDS.sleep(15);

        client.disconnect();
        server.shutdown();
    }
}