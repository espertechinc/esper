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

/**
 * Ctor
 */
public class ThreadingOption {
    /**
     * Public access.
     */
    public static boolean isThreadingEnabled = false;

    /**
     * Sets the thread option on.
     *
     * @param threadingEnabled option on
     */
    public static void setThreadingEnabled(Boolean threadingEnabled) {
        isThreadingEnabled = threadingEnabled;
    }

    /**
     * Returns true when threading is enabled
     *
     * @return indicator
     */
    public static boolean isThreadingEnabled() {
        return isThreadingEnabled;
    }
}
