package netty.examples.remoting.core;

import io.netty.channel.Channel;
import netty.examples.commons.protocol.RemotingCommand;

/**
 * @author no-today
 * @date 2022/05/31 16:01
 */
public class RequestTask implements Runnable {

    private final Runnable runnable;
    private final long createTimestamp = System.currentTimeMillis();
    private final Channel channel;
    private final RemotingCommand request;
    private boolean stopRun = false;

    public RequestTask(Runnable runnable, Channel channel, RemotingCommand request) {
        this.runnable = runnable;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        if (stopRun) return;
        runnable.run();
    }

    public void setStopRun(boolean stopRun) {
        this.stopRun = stopRun;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public Channel getChannel() {
        return channel;
    }

    public RemotingCommand getRequest() {
        return request;
    }
}
