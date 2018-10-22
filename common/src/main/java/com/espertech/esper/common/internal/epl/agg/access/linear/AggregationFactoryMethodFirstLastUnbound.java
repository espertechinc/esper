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
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationFactoryMethodBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.agg.method.firstlastever.AggregatorFirstEver;
import com.espertech.esper.common.internal.epl.agg.method.firstlastever.AggregatorLastEver;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class AggregationFactoryMethodFirstLastUnbound extends AggregationFactoryMethodBase {
    protected final ExprAggMultiFunctionLinearAccessNode parent;
    private final EventType collectionEventType;
    private final Class resultType;
    private final int streamNum;
    protected final boolean hasFilter;
    private AggregatorMethod aggregator;

    public AggregationFactoryMethodFirstLastUnbound(ExprAggMultiFunctionLinearAccessNode parent, EventType collectionEventType, Class resultType, int streamNum, boolean hasFilter) {
        this.parent = parent;
        this.collectionEventType = collectionEventType;
        this.resultType = resultType;
        this.streamNum = streamNum;
        this.hasFilter = hasFilter;
    }

    public Class getResultType() {
        return resultType;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        if (parent.getStateType() == AggregationAccessorLinearType.FIRST) {
            aggregator = new AggregatorFirstEver(this, col, rowCtor, membersColumnized, classScope, null, hasFilter, parent.getOptionalFilter(), resultType);
        } else if (parent.getStateType() == AggregationAccessorLinearType.LAST) {
            aggregator = new AggregatorLastEver(this, col, rowCtor, membersColumnized, classScope, null, hasFilter, parent.getOptionalFilter(), resultType);
        } else {
            throw new RuntimeException("Window aggregation function is not available");
        }
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        throw new UnsupportedOperationException("Not available as linear-access first/last is not used with tables");
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }
}