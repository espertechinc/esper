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
package com.espertech.esper.epl.join.pollindex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.join.table.EventTable;

import java.util.List;

/**
 * A strategy for converting a poll-result into a potentially indexed table.
 * <p>
 * Some implementations may decide to not index the poll result and simply hold a reference to the result.
 * Other implementations may use predetermined index properties to index the poll result for faster lookup.
 */
public interface PollResultIndexingStrategy {
    /**
     * Build and index of a poll result.
     *
     * @param pollResult       result of a poll operation
     * @param isActiveCache    true to indicate that caching is active and therefore index building makes sense as
     *                         the index structure is not a throw-away.
     * @param statementContext statement context
     * @return indexed collection of poll results
     */
    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, StatementContext statementContext);

    public String toQueryPlan();
}
