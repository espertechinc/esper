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
package com.espertech.esper.pattern.guard;

import com.espertech.esper.pattern.PatternAgentInstanceContext;

/**
 * Receiver for quit events for use by guards.
 */
public interface Quitable {
    /**
     * Indicate guard quitted.
     */
    public void guardQuit();

    /**
     * Retains the pattern context with relevant pattern and statement-level services.
     * <p>
     * The pattern context is the same context as provided to the guard factory and
     * is provided by the quitable so the guard instance does not need to retain the pattern context.
     *
     * @return pattern context
     */
    public PatternAgentInstanceContext getContext();
}
