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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;

/**
 * Interface for a statement-level service for coordinating the insert/remove stream generation,
 * native deliver to subscribers and the presence/absence of listener or subscribers to a statement.
 */
public interface StatementResultService {
    String getStatementName();

    ThreadLocal<StatementDispatchTLEntry> getDispatchTL();

    void execute(StatementDispatchTLEntry dispatchTLEntry);

    void indicate(UniformPair<EventBean[]> results, StatementDispatchTLEntry dispatchTLEntry);

    boolean isMakeSynthetic();

    boolean isMakeNatural();

    void clearDeliveriesRemoveStream(EventBean[] removedEvents);
}
