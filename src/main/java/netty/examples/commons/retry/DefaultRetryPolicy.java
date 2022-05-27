package netty.examples.commons.retry;

import io.netty.channel.Channel;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author no-today
 * @date 2022/05/25 10:18
 */
public class DefaultRetryPolicy implements RetryPolicy {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static RetryPolicy instance;

    private DefaultRetryPolicy() {
    }

    public static RetryPolicy getInstance() {
        if (initialized.compareAndSet(false, true)) {
            instance = new DefaultRetryPolicy();
        }
        return instance;
    }

    public static Channel retryCall(Logger log, Callable<Channel> callable) {
        int i = 0;
        while (true) {
            try {
                return callable.call();
            } catch (Exception e) {
                if (i > 5) i = 0;
                long sleepTimeMs = DefaultRetryPolicy.getInstance().getSleepTimeMs(i++);
                log.info("Connect failed, sleep {}ms", sleepTimeMs);

                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public long getSleepTimeMs(int retryCount) {
        return (1L << retryCount) * 1000;
    }
}
