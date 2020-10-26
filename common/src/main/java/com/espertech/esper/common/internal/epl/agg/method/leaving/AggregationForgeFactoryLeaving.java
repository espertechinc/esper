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
package com.espertech.esper.common.internal.epl.agg.method.leaving;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprLeavingAggNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class AggregationForgeFactoryLeaving extends AggregationForgeFactoryBase {
    protected final ExprLeavingAggNode parent;
    protected final AggregatorLeaving forge;

    public AggregationForgeFactoryLeaving(ExprLeavingAggNode parent) {
        this.parent = parent;
        this.forge = new AggregatorLeaving(this);
    }

    public EPType getResultType() {
        return EPTypePremade.BOOLEANBOXED.getEPType();
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregatorMethod getAggregator() {
        return forge;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationLeaving(parent.isDistinct(), parent.getOptionalFilter() != null, EPTypePremade.BOOLEANPRIMITIVE.getEPType());
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }
}
