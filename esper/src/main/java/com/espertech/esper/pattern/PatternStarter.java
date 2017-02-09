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
package com.espertech.esper.pattern;

/**
 * Interface for observing when an event expression needs to start (by adding the first listener).
 * The publishing event expression supplies the callback used for indicating matches. The implementation supplies
 * as a return value the callback to use to stop the event expression.
 */
public interface PatternStarter {
    /**
     * An event expression was started and supplies the callback to use when matching events appear.
     * Returns the callback to use to stop the event expression.
     *
     * @param matchCallback         must be supplied to indicate what to call when the expression turns true
     * @param context               is the context for handles to services required for evaluation.
     * @param isRecoveringResilient true for recovering
     * @return a callback to stop the expression again
     */
    public PatternStopCallback start(PatternMatchCallback matchCallback,
                                     PatternContext context,
                                     boolean isRecoveringResilient);
}
