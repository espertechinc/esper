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
package com.espertech.esperio.kafka;

import java.util.concurrent.ThreadFactory;

public class EsperIOKafkaInputThreadFactory implements ThreadFactory {
    private final ThreadGroup threadGroup;

    public EsperIOKafkaInputThreadFactory(String engineURI) {
        this.threadGroup = new ThreadGroup("esperio__kafkainput__" + engineURI);
    }

    public Thread newThread(Runnable runnable) {
        String name = threadGroup.getName();
        Thread t = new Thread(threadGroup, runnable, name);
        t.setDaemon(true);
        return t;
    }
}
