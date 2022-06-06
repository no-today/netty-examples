package netty.examples.remoting.core;

import lombok.Getter;
import lombok.Setter;

/**
 * @author no-today
 * @date 2022/06/06 11:03
 */
@Getter
@Setter
public class NettyClientConfig {

    private String host = "127.0.0.1";
    private int port = 7879;

    private int workerThreads = 4;
    private int callbackExecutorThreads = Runtime.getRuntime().availableProcessors();

    private int asyncSemaphoreValue = 10;
    private int onewaySemaphoreValue = 10;

    private int connectTimeoutMillis = 3000;
    private int channelMaxIdleTimeSeconds = 120;
    private boolean closeSocketIfTimeout = true;

    private int socketSndBufSize = 0;
    private int SocketRcvBufSize = 0;

    private int writeBufferHighWaterMark = 0;
    private int writeBufferLowWaterMark = 0;
}
