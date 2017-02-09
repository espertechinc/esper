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
package com.espertech.esper.core.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread factory for threading options.
 */
public class EngineThreadFactory implements java.util.concurrent.ThreadFactory {
    private static final Logger log = LoggerFactory.getLogger(EngineThreadFactory.class);
    private final String engineURI;
    private final String prefix;
    private final ThreadGroup threadGroup;
    private final int threadPriority;
    private int currThreadCount;

    /**
     * Ctor.
     *
     * @param engineURI   engine URI
     * @param prefix      prefix for thread names
     * @param threadGroup thread group
     * @param threadPrio  priority to use
     */
    public EngineThreadFactory(String engineURI, String prefix, ThreadGroup threadGroup, int threadPrio) {
        if (engineURI == null) {
            this.engineURI = "default";
        } else {
            this.engineURI = engineURI;
        }
        this.prefix = prefix;
        this.threadGroup = threadGroup;
        this.threadPriority = threadPrio;
    }

    public Thread newThread(Runnable runnable) {
        String name = "com.espertech.esper." + prefix + "-" + engineURI + "-" + currThreadCount;
        currThreadCount++;
        Thread t = new Thread(threadGroup, runnable, name);
        t.setDaemon(true);
        t.setPriority(threadPriority);

        if (log.isDebugEnabled()) {
            log.debug("Creating thread '" + name + "' : " + t + " priority " + threadPriority);
        }
        return t;
    }
}
