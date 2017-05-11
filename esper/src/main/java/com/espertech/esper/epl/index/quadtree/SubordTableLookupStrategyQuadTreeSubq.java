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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordTableLookupStrategy;

import java.util.Collection;

public class SubordTableLookupStrategyQuadTreeSubq extends SubordTableLookupStrategyQuadTreeBase implements SubordTableLookupStrategy {
    private final EventBean[] events;

    public SubordTableLookupStrategyQuadTreeSubq(EventTableQuadTree index, SubordTableLookupStrategyFactoryQuadTree factory, int numStreamsOuter) {
        super(index, factory);
        this.events = new EventBean[numStreamsOuter + 1];
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
        return lookupInternal(events, context);
    }
}
