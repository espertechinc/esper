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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprNodeRenderable;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

public class SelectExprProcessorTypableMapForge implements SelectExprProcessorTypableForge {
    protected final EventType mapType;
    protected final ExprForge innerForge;
    protected  final EventAdapterService eventAdapterService;

    public SelectExprProcessorTypableMapForge(EventType mapType, ExprForge innerForge, EventAdapterService eventAdapterService) {
        this.mapType = mapType;
        this.innerForge = innerForge;
        this.eventAdapterService = eventAdapterService;
    }

    public ExprEvaluator getExprEvaluator() {
        return new SelectExprProcessorTypableMapEval(this, innerForge.getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return SelectExprProcessorTypableMapEval.codegen(this, context, params);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getUnderlyingEvaluationType() {
        return Map.class;
    }

    public Class getEvaluationType() {
        return EventBean.class;
    }

    public ExprForge getInnerForge() {
        return innerForge;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return innerForge.getForgeRenderable();
    }
}
