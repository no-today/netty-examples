package netty.examples.commons.retry;

/**
 * @author no-today
 * @date 2022/05/25 10:07
 */
public interface RetryPolicy {

    long getSleepTimeMs(int retryCount);
}
