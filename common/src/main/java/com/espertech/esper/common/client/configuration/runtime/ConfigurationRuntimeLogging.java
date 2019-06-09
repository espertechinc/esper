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
package com.espertech.esper.common.client.configuration.runtime;

import java.io.Serializable;

/**
 * Holds view logging settings other then the Apache commons or Log4J settings.
 */
public class ConfigurationRuntimeLogging implements Serializable {
    private static final long serialVersionUID = 2406269988569122375L;

    private boolean enableExecutionDebug;
    private boolean enableTimerDebug;
    private String auditPattern;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationRuntimeLogging() {
        enableExecutionDebug = false;
        enableTimerDebug = true;
    }

    /**
     * Returns true if execution path debug logging is enabled.
     * <p>
     * Only if this flag is set to true, in addition to LOG4J settings set to DEBUG, does a runtime instance,
     * produce debug out.
     *
     * @return true if debug logging through Log4j is enabled for any event processing execution paths
     */
    public boolean isEnableExecutionDebug() {
        return enableExecutionDebug;
    }

    /**
     * Set the debug flag for debugging the execution path, in which case the runtime logs
     * to Log4j in debug-level during execution.
     *
     * @param enableExecutionDebug false to disable debug logging in the execution path, true to enable
     */
    public void setEnableExecutionDebug(boolean enableExecutionDebug) {
        this.enableExecutionDebug = enableExecutionDebug;
    }

    /**
     * Returns true if timer debug level logging is enabled (true by default).
     * <p>
     * Set this value to false to reduce the debug-level logging output for the timer thread(s).
     * For use only when debug-level logging is enabled.
     *
     * @return indicator whether timer execution is noisy in debug or not
     */
    public boolean isEnableTimerDebug() {
        return enableTimerDebug;
    }

    /**
     * Set this value to false to reduce the debug-level logging output for the timer thread(s).
     * For use only when debug-level logging is enabled.
     *
     * @param enableTimerDebug indicator whether timer execution is noisy in debug or not (true is noisy)
     */
    public void setEnableTimerDebug(boolean enableTimerDebug) {
        this.enableTimerDebug = enableTimerDebug;
    }

    /**
     * Returns the pattern that formats audit logs.
     * <p>
     * Available conversion characters are:
     * </p>
     * <p>
     * %m      - Used to output the audit message.
     * %s      - Used to output the statement name.
     * %u      - Used to output the runtime URI.
     * </p>
     *
     * @return audit formatting pattern
     */
    public String getAuditPattern() {
        return auditPattern;
    }

    /**
     * Sets the audit formatting pattern that formats audit logs, or null if using default format.
     *
     * @param auditPattern pattern to use
     */
    public void setAuditPattern(String auditPattern) {
        this.auditPattern = auditPattern;
    }
}
