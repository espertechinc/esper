/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esperio.db.core;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esperio.db.config.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ExecutorServices {
    private final static Logger log = LoggerFactory.getLogger(ExecutorServices.class);

    private static final java.util.concurrent.Executor EXEC_SAME_THREAD = new ExecutorSameThread();

    private final Map<String, ExecutorService> services;

    public ExecutorServices(EPServiceProviderSPI spi, Map<String, Executor> workQueue) {
        this.services = new HashMap<String, ExecutorService>();

        for (Map.Entry<String, Executor> entry : workQueue.entrySet()) {
            Executor queue = entry.getValue();

            if (queue.getNumThreads() <= 0) {
                continue;
            }

            LinkedBlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<Runnable>();
            ExecutorService service = new ThreadPoolExecutor(queue.getNumThreads(), queue.getNumThreads(), 1000, TimeUnit.SECONDS, runnableQueue);
            services.put(entry.getKey(), service);
        }

        try {
            spi.getContext().bind("EsperIODBAdapter/ExecutorServices", this);
        } catch (NamingException e) {
            log.error("Error binding executor service: " + e.getMessage(), e);
        }
    }

    public java.util.concurrent.Executor getConfiguredExecutor(String workqueueName) throws ConfigurationException {
        if (workqueueName == null) {
            return EXEC_SAME_THREAD;
        }
        ExecutorService svc = services.get(workqueueName);
        if (svc == null) {
            throw new ConfigurationException("Executor by name '" + workqueueName + "' has not been defined");
        }
        return svc;
    }

    public BlockingQueue<Runnable> getQueue(String name) {
        if (!services.containsKey(name)) {
            return null;
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) services.get(name);
        return executor.getQueue();
    }

    public java.util.concurrent.Executor getExecutor(String name) {
        if (!services.containsKey(name)) {
            return null;
        }
        return services.get(name);
    }

    public void destroy() {
        for (Map.Entry<String, ExecutorService> entry : services.entrySet()) {
            entry.getValue().shutdown();
        }
    }
}
