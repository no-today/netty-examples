package netty.examples.remoting.core;

import lombok.Getter;
import lombok.Setter;

/**
 * @author no-today
 * @date 2022/06/06 14:45
 */
@Getter
@Setter
public class NettyServerConfig {

    private int listenPort = 7879;

    private int bossThreads = 3;
    private int workerThreads = 8;
    private int callbackExecutorThreads = 0;

    private int onewaySemaphoreValue = 256;
    private int asyncSemaphoreValue = 64;
    private int channelMaxIdleTimeSeconds = 120;

    private int socketSndBufSize = 0;
    private int socketRcvBufSize = 0;
    private int writeBufferHighWaterMark = 0;
    private int writeBufferLowWaterMark = 0;
    private int socketBacklog = 1024;
    private boolean serverPooledByteBufAllocatorEnable = true;
}
