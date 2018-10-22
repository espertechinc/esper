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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.EventBean;

/**
 * Interface to receive notification from an runtime that an event that has been sent into the runtimeor that
 * has been generated via insert-into has not been matched to any statement, considering all started statement's
 * event stream filter criteria (not considering where-clause and having-clauses).
 * <p>
 * Also see {@link EPEventService#setUnmatchedListener(UnmatchedListener)}.
 */
public interface UnmatchedListener {
    /**
     * Called upon the runtimeencountering an event not matched to any of the event stream filters
     * of any active statement.
     *
     * @param theEvent unmatched event
     */
    public void update(EventBean theEvent);
}
