package com.espertech.esper.example.trivia;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ThreadFactory;

public class SimulatorThreadFactory implements ThreadFactory {
    private static Log log = LogFactory.getLog(SimulatorThreadFactory.class);

    private final String engineURI;
    private final String name;
    private final ThreadGroup threadGroup;
    private int currThreadCount;

    /**
     * Ctor.
     * @param engineURI engine URI
     * @param name thread name
     */
    public SimulatorThreadFactory(String engineURI, String name)
    {
        this.engineURI = engineURI;
        this.name = name;
        String threadGroupName = "com.espertech.esper-" + name + "-" + engineURI + "-ThreadGroup";
        this.threadGroup = new ThreadGroup(threadGroupName);
    }

    public Thread newThread(Runnable runnable)
    {
        String threadName = "com.espertech.esper-" + name + "-" + engineURI + "-Thread-" + currThreadCount;
        currThreadCount++;
        Thread t = new Thread(threadGroup, runnable, threadName);
        t.setDaemon(true);

        if (log.isDebugEnabled())
        {
            log.debug(".newThread Creating thread '" + threadName + " : " + t);
        }
        return t;
    }
}
