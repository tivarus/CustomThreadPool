package ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskPoolExecutor implements CustomExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TaskPoolExecutor.class);

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int minSpareThreads;

    private final List<WorkerExecutor> workers;
    private final List<BlockingQueue<Runnable>> taskQueues;

    private final AtomicInteger queueIndex = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    public TaskPoolExecutor(int corePoolSize, int maxPoolSize, int queueSize,
                            long keepAliveTime, TimeUnit timeUnit, int minSpareThreads) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.minSpareThreads = minSpareThreads;

        this.taskQueues = new ArrayList<>();
        this.workers = new ArrayList<>();

        for (int i = 0; i < corePoolSize; i++) {
            createWorker(i);
        }
    }

    public TaskPoolExecutor(int corePoolSize, int maxPoolSize, int queueSize,
                            long keepAliveTime) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = TimeUnit.SECONDS;
        this.minSpareThreads = 2;

        this.taskQueues = new ArrayList<>();
        this.workers = new ArrayList<>();

        for (int i = 0; i < corePoolSize; i++) {
            createWorker(i);
        }
    }
    private void createWorker(int index) {
        if (workers.size() >= maxPoolSize) {
            throw new IllegalStateException("Cannot create worker - maxPoolSize reached");
        }
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
        WorkerExecutor worker = new WorkerExecutor(queue, index, keepAliveTime, timeUnit);
        taskQueues.add(queue);
        workers.add(worker);
        new Thread(worker, "Worker " + index).start();
    }

    @Override
    public void execute(Runnable command) {
        int index = 0;
        if (isShutdown) {
            throw new RejectedExecutionException("is Shutdown");
        }
        if (taskQueues.size() > 0)
        {
            index = queueIndex.getAndIncrement() % taskQueues.size();
            BlockingQueue<Runnable> queue = taskQueues.get(index);
            if (queue.offer(command)) {
                logger.debug("Task push to the queue {}", index);
                return;
            }
        }
        synchronized (this) {
            if (workers.size() < maxPoolSize) {
                int newWorkerIndex = workers.size();
                createWorker(newWorkerIndex);
                logger.info("Created new worker {}", newWorkerIndex);

                BlockingQueue<Runnable> newQueue = taskQueues.get(newWorkerIndex);
                if (newQueue.offer(command)) {
                    logger.debug("Task pushed to new queue {}", newWorkerIndex);
                    return;
                }
            }
        }
        throw new RejectedExecutionException("All queues full and maxPoolSize reached");
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        if (isShutdown) {
            throw new RejectedExecutionException("is Shutdown");
        }

        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        logger.info("shutdown");
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        logger.info("shutdownNow");
        for (WorkerExecutor worker : workers) {
            worker.stop();
        }
    }
}
