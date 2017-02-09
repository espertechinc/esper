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
package com.espertech.esper.util;

/**
 * Utility class that control debug-level logging in the execution path
 * beyond which is controlled by Log4j.
 * <p>
 * As Log4j will return true to the "log.isDebugEnabled()" call when
 * there is no log4j configuration, this leaves the door open to poor
 * execution time performance if one forgets the log4j config file.
 * <p>
 * Note that a static variable control this setting and therefore
 * the debug-enable applies to engines within the module or VM.
 */
public class ExecutionPathDebugLog {
    /**
     * Public access.
     */
    public static boolean isDebugEnabled = false;

    /**
     * Public access.
     */
    public static boolean isTimerDebugEnabled = true;

    /**
     * Sets execution path debug logging.
     *
     * @param debugEnabled true for allowing Log4j debug log messages to be generated for the execution path
     */
    public static void setDebugEnabled(Boolean debugEnabled) {
        isDebugEnabled = debugEnabled;
    }

    /**
     * Sets debug logging for timer.
     *
     * @param timerDebugEnabled true for allowing Log4j debug log messages for regular timer execution
     */
    public static void setTimerDebugEnabled(Boolean timerDebugEnabled) {
        isTimerDebugEnabled = timerDebugEnabled;
    }
}
