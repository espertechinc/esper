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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JoinSetComposerUtil {
    private final static EventTable[] EMPTY = new EventTable[0];

    public static boolean isNonUnidirectionalNonSelf(boolean isOuterJoins, boolean isUnidirectional, boolean isPureSelfJoin) {
        return (!isUnidirectional) &&
                (!isPureSelfJoin || isOuterJoins);
    }

    public static void filter(ExprEvaluator filterExprNode, Set<MultiKeyArrayOfKeys<EventBean>> events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (Iterator<MultiKeyArrayOfKeys<EventBean>> it = events.iterator(); it.hasNext(); ) {
            MultiKeyArrayOfKeys<EventBean> key = it.next();
            EventBean[] eventArr = key.getArray();

            Boolean matched = (Boolean) filterExprNode.evaluate(eventArr, isNewData, exprEvaluatorContext);
            if ((matched == null) || (!matched)) {
                it.remove();
            }
        }
    }

    public static EventTable[][] toArray(Map<TableLookupIndexReqKey, EventTable>[] repositories) {
        return toArray(repositories, repositories.length);
    }

    public static EventTable[][] toArray(Map<TableLookupIndexReqKey, EventTable>[] repositories, int length) {
        if (repositories == null) {
            return getDefaultTablesArray(length);
        }
        EventTable[][] tables = new EventTable[repositories.length][];
        for (int i = 0; i < repositories.length; i++) {
            tables[i] = toArray(repositories[i]);
        }
        return tables;
    }

    private static EventTable[] toArray(Map<TableLookupIndexReqKey, EventTable> repository) {
        if (repository == null) {
            return EMPTY;
        }
        EventTable[] tables = new EventTable[repository.size()];
        int count = 0;
        for (Map.Entry<TableLookupIndexReqKey, EventTable> entries : repository.entrySet()) {
            tables[count] = entries.getValue();
            count++;
        }
        return tables;
    }

    private static EventTable[][] getDefaultTablesArray(int length) {
        EventTable[][] result = new EventTable[length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = EMPTY;
        }
        return result;
    }
}