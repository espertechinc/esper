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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationPortableValidationLinear implements AggregationPortableValidation {
    private EventType containedEventType;

    public AggregationPortableValidationLinear() {
    }

    public AggregationPortableValidationLinear(EventType containedEventType) {
        this.containedEventType = containedEventType;
    }

    public void setContainedEventType(EventType containedEventType) {
        this.containedEventType = containedEventType;
    }

    public EventType getContainedEventType() {
        return containedEventType;
    }

    public void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);
        AggregationPortableValidationLinear other = (AggregationPortableValidationLinear) intoTableAgg;
        AggregationValidationUtil.validateEventType(this.containedEventType, other.getContainedEventType());
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationPortableValidationLinear.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationPortableValidationLinear.class, "v", newInstance(AggregationPortableValidationLinear.class))
                .exprDotMethod(ref("v"), "setContainedEventType", EventTypeUtility.resolveTypeCodegen(containedEventType, symbols.getAddInitSvc(method)))
                .methodReturn(ref("v"));
        return localMethod(method);
    }
}
