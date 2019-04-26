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
package com.espertech.esper.common.internal.epl.agg.method.rate;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprRateAggNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;

public class AggregationForgeFactoryRate extends AggregationForgeFactoryBase {
    protected final ExprRateAggNode parent;
    protected final boolean isEver;
    protected final long intervalTime;
    protected final TimeAbacus timeAbacus;
    protected AggregatorMethod aggregator;

    public AggregationForgeFactoryRate(ExprRateAggNode parent, boolean isEver, long intervalTime, TimeAbacus timeAbacus) {
        this.parent = parent;
        this.isEver = isEver;
        this.intervalTime = intervalTime;
        this.timeAbacus = timeAbacus;
    }

    public Class getResultType() {
        return Double.class;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        if (isEver) {
            aggregator = new AggregatorRateEver(this, col, rowCtor, membersColumnized, classScope, null, null, false, parent.getOptionalFilter());
        } else {
            aggregator = new AggregatorRate(this, col, rowCtor, membersColumnized, classScope, null, null, false, parent.getOptionalFilter());
        }
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public ExprRateAggNode getParent() {
        return parent;
    }

    public boolean isEver() {
        return isEver;
    }

    public long getIntervalTime() {
        return intervalTime;
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationRate(parent.isDistinct(), parent.getOptionalFilter() != null, int.class, intervalTime);
    }
}