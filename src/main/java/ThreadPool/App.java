package ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.RejectedExecutionException;

public class App {

    static Logger logger = LoggerFactory.getLogger(App.class);
    static int taskCount = 200;
    public static void main(String[] args) {
        TaskPoolExecutor executor = new TaskPoolExecutor(4, 10, 50, 5);

        int taskBlockCnt = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            try {
                executor.execute(() -> {
                    logger.info("Task {} start", taskId);
                    try {
                        Thread.sleep(100); // имитация работы
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    logger.info("Task {} end", taskId);
                });
            } catch (RejectedExecutionException e) {
                logger.info("Task {} block", taskId);
                taskBlockCnt++;
            }
        }

        long endTime = System.currentTimeMillis();
        executor.shutdown();

        try {
            Thread.sleep(2000); // ждём завершения потоков
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        testResult(startTime, endTime, taskBlockCnt);
    }

    private static void testResult(long startTime, long endTime, int rejectedTasks) {
        long totalTime = endTime - startTime;
        int executedTasks = taskCount - rejectedTasks;
        double avgTime = executedTasks > 0 ? (double) totalTime / executedTasks : 0.0;
        DecimalFormat df = new DecimalFormat("#.##");

        logger.info("!TestResult!");
        logger.info("Work time: {} ", totalTime);
        logger.info("Task completed: {}", executedTasks);
        logger.info("Task blocked: {}", rejectedTasks);
        logger.info("Time per task count: {}", df.format(avgTime));
    }
}

