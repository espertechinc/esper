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
package com.espertech.esper.example.trivia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

public class SimulatorThreadFactory implements ThreadFactory {
    private final static Logger log = LoggerFactory.getLogger(SimulatorThreadFactory.class);

    private final String runtimeURI;
    private final String name;
    private final ThreadGroup threadGroup;
    private int currThreadCount;

    /**
     * Ctor.
     *
     * @param runtimeURI runtime URI
     * @param name       thread name
     */
    public SimulatorThreadFactory(String runtimeURI, String name) {
        this.runtimeURI = runtimeURI;
        this.name = name;
        String threadGroupName = "com.espertech.esper-" + name + "-" + runtimeURI + "-ThreadGroup";
        this.threadGroup = new ThreadGroup(threadGroupName);
    }

    public Thread newThread(Runnable runnable) {
        String threadName = "com.espertech.esper-" + name + "-" + runtimeURI + "-Thread-" + currThreadCount;
        currThreadCount++;
        Thread t = new Thread(threadGroup, runnable, threadName);
        t.setDaemon(true);

        if (log.isDebugEnabled()) {
            log.debug(".newThread Creating thread '" + threadName + " : " + t);
        }
        return t;
    }
}
