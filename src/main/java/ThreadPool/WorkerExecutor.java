package ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class WorkerExecutor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WorkerExecutor.class);

    private final BlockingQueue<Runnable> taskQueue;
    private final int workerId;

    private final long keepAliveTime;
    private final TimeUnit timeUnit;

    private volatile boolean running = true;

    public WorkerExecutor(BlockingQueue<Runnable> taskQueue, int workerId, long keepAliveTime, TimeUnit timeUnit) {
        this.taskQueue = taskQueue;
        this.workerId = workerId;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
    }

    @Override
    public void run() {
        logger.info("Worker {} start", workerId);
        try {
            while (running) {
                Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                if (task != null) {
                    logger.info("Worker{} - get task", workerId);
                    try {
                        task.run();
                    } catch (Exception e) {
                        logger.error("Worker {} - task {} execute error", workerId, e.getMessage());
                    }
                } else {
                    logger.info("Worker {} timeout idle stop", workerId);
                    running = false;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Worker {} shutdown", workerId);
        }
    }

    public void stop() {
        this.running = false;
    }
}

