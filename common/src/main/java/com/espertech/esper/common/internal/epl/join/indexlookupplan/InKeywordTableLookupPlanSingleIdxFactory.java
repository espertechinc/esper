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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.inkeyword.InKeywordSingleTableLookupStrategyExpr;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;

/**
 * Plan to perform an indexed table lookup.
 */
public class InKeywordTableLookupPlanSingleIdxFactory extends TableLookupPlan {
    private ExprEvaluator[] expressions;

    public InKeywordTableLookupPlanSingleIdxFactory(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNums, ExprEvaluator[] expressions) {
        super(lookupStream, indexedStream, indexNums);
        this.expressions = expressions;
    }

    public JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes) {
        PropertyHashedEventTable index = (PropertyHashedEventTable) eventTable[0];
        return new InKeywordSingleTableLookupStrategyExpr(this, index);
    }

    public ExprEvaluator[] getExpressions() {
        return expressions;
    }
}
