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
package com.espertech.esper.common.internal.epl.join.indexlookupplan;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.hash.IndexedTableLookupStrategyHashedExpr;
import com.espertech.esper.common.internal.epl.join.exec.hash.IndexedTableLookupStrategyHashedProp;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;

/**
 * Plan to perform an indexed table lookup.
 */
public class IndexedTableLookupPlanHashedOnlyFactory extends TableLookupPlan {
    protected final ExprEvaluator exprEvaluator;
    protected final EventPropertyValueGetter eventPropertyValueGetter;

    public IndexedTableLookupPlanHashedOnlyFactory(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNum, ExprEvaluator exprEvaluator) {
        super(lookupStream, indexedStream, indexNum);
        this.exprEvaluator = exprEvaluator;
        this.eventPropertyValueGetter = null;
    }

    public IndexedTableLookupPlanHashedOnlyFactory(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNum, EventPropertyValueGetter eventPropertyValueGetter) {
        super(lookupStream, indexedStream, indexNum);
        this.exprEvaluator = null;
        this.eventPropertyValueGetter = eventPropertyValueGetter;
    }

    public ExprEvaluator getExprEvaluator() {
        return exprEvaluator;
    }

    public EventPropertyValueGetter getEventPropertyValueGetter() {
        return eventPropertyValueGetter;
    }

    protected JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTables, EventType[] eventTypes) {
        PropertyHashedEventTable index = (PropertyHashedEventTable) eventTables[0];
        if (eventPropertyValueGetter != null) {
            return new IndexedTableLookupStrategyHashedProp(this, index);
        } else {
            return new IndexedTableLookupStrategyHashedExpr(this, index, eventTypes.length);
        }
    }
}
