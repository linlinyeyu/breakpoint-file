package com.ybliu.breakpoint.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author linlinyeyu
 */
public class FileThreadPool extends ThreadPoolExecutor{
    private FileThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    private static class SingletonInstance {
        private static final FileThreadPool INSTANCE = new FileThreadPool(5,10,15,TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    public static FileThreadPool getInstance() {
        return SingletonInstance.INSTANCE;
    }
}
