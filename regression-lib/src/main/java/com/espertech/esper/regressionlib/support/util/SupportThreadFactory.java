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
package com.espertech.esper.regressionlib.support.util;

import java.util.concurrent.ThreadFactory;

public class SupportThreadFactory implements ThreadFactory {
    private final Class user;

    public SupportThreadFactory(Class user) {
        this.user = user;
    }

    public Thread newThread(Runnable r) {
        String name = user.getSimpleName();
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }
}
