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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;

/**
 * Interface for matching an event instance based on the event's property values to
 * filters, specifically filter parameter constants or ranges.
 */
public interface EventEvaluator {
    /**
     * Perform the matching of an event based on the event property values,
     * adding any callbacks for matches found to the matches list.
     *
     * @param theEvent is the event object wrapper to obtain event property values from
     * @param matches  accumulates the matching filter callbacks
     */
    void matchEvent(EventBean theEvent, Collection<FilterHandle> matches);

    void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack);
}
