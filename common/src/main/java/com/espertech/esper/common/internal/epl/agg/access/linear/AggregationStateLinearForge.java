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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class AggregationStateLinearForge implements AggregationStateFactoryForge {

    protected final ExprAggMultiFunctionLinearAccessNode expr;
    protected final int streamNum;
    protected final ExprForge optionalFilter;
    private AggregatorAccessLinear aggregator;

    public AggregationStateLinearForge(ExprAggMultiFunctionLinearAccessNode expr, int streamNum, ExprForge optionalFilter) {
        this.expr = expr;
        this.streamNum = streamNum;
        this.optionalFilter = optionalFilter;
    }

    public void initAccessForge(int col, boolean join, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        if (!join) {
            aggregator = new AggregatorAccessLinearNonJoin(this, col, rowCtor, membersColumnized, classScope, expr.getOptionalFilter());
        } else {
            aggregator = new AggregatorAccessLinearJoin(this, col, rowCtor, membersColumnized, classScope, expr.getOptionalFilter());
        }
    }

    public CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        return constantNull(); // not implemented for linear state as AggregationTableAccessAggReader can simple call "getCollectionOfEvents"
    }

    public AggregatorAccess getAggregator() {
        return aggregator;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprForge getOptionalFilter() {
        return optionalFilter;
    }

    public AggregatorAccessLinear getAggregatorLinear() {
        return aggregator;
    }

    public EventType getEventType() {
        return expr.getStreamType();
    }

    public Class getClassType() {
        return expr.getComponentTypeCollection();
    }

    public ExprNode getExpression() {
        return expr;
    }
}
