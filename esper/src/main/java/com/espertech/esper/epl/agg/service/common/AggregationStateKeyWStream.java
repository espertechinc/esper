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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.EventTypeUtility;

public class AggregationStateKeyWStream implements AggregationStateKey {
    private final int streamNum;
    private final EventType eventType;
    private final AggregationStateTypeWStream stateType;
    private final ExprNode[] criteraExprNodes;
    private final ExprNode filterExprNode;

    public AggregationStateKeyWStream(int streamNum, EventType eventType, AggregationStateTypeWStream stateType, ExprNode[] criteraExprNodes, ExprNode filterExprNode) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.stateType = stateType;
        this.criteraExprNodes = criteraExprNodes;
        this.filterExprNode = filterExprNode;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregationStateKeyWStream that = (AggregationStateKeyWStream) o;

        if (streamNum != that.streamNum) return false;
        if (stateType != that.stateType) return false;
        if (!ExprNodeUtilityCore.deepEquals(criteraExprNodes, that.criteraExprNodes, false)) return false;
        if (eventType != null) {
            if (that.eventType == null) {
                return false;
            }
            if (!EventTypeUtility.isTypeOrSubTypeOf(that.eventType, eventType)) return false;
        }

        if (filterExprNode == null) {
            return that.filterExprNode == null;
        }
        return that.filterExprNode != null && ExprNodeUtilityCore.deepEquals(filterExprNode, that.filterExprNode, false);
    }

    public int hashCode() {
        int result = streamNum;
        result = 31 * result + stateType.hashCode();
        return result;
    }
}
