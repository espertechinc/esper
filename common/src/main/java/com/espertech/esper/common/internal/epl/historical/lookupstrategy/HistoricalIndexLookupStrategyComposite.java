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
package com.espertech.esper.common.internal.epl.historical.lookupstrategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTable;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexQuery;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexQueryFactory;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HistoricalIndexLookupStrategyComposite implements HistoricalIndexLookupStrategy {

    private int lookupStream;
    private ExprEvaluator hashGetter;
    private QueryGraphValueEntryRange[] rangeProps;
    private CompositeIndexQuery chain;

    public Iterator<EventBean> lookup(EventBean lookupEvent, EventTable[] index, ExprEvaluatorContext context) {
        if (index[0] instanceof PropertyCompositeEventTable) {
            PropertyCompositeEventTable idx = (PropertyCompositeEventTable) index[0];
            Map<Object, Object> map = idx.getIndex();
            Set<EventBean> events = chain.get(lookupEvent, map, context, idx.getPostProcessor());
            if (events != null) {
                return events.iterator();
            }
            return null;
        }
        return index[0].iterator();
    }

    public void setLookupStream(int lookupStream) {
        this.lookupStream = lookupStream;
    }

    public void setHashGetter(ExprEvaluator hashGetter) {
        this.hashGetter = hashGetter;
    }

    public void setRangeProps(QueryGraphValueEntryRange[] rangeProps) {
        this.rangeProps = rangeProps;
    }

    public void setChain(CompositeIndexQuery chain) {
        this.chain = chain;
    }

    public void init() {
        chain = CompositeIndexQueryFactory.makeJoinSingleLookupStream(false, lookupStream, hashGetter, rangeProps);
    }
}
