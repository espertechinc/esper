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
package com.espertech.esper.common.internal.filtersvc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.Collection;

/**
 * Interface for a callback method to be called when an event matches a filter specification. Provided
 * as a convenience for use as a filter handle for registering with the {@link FilterService}.
 */
public interface FilterHandleCallback {
    /**
     * Indicate that an event was evaluated by the {@link FilterService}
     * which matches the filter specification {@link FilterSpecActivatable} associated with this callback.
     *
     * @param theEvent       - the event received that matches the filter specification
     * @param allStmtMatches - collection of matches that represent all matches for the same statement
     */
    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches);

    /**
     * Returns true if the filter applies to subselects.
     *
     * @return subselect filter
     */
    public boolean isSubSelect();
}
