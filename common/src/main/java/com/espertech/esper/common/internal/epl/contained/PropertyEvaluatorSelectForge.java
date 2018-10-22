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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorDescriptor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property evaluator that considers a select-clauses and relies
 * on an accumulative property evaluator that presents events for all columns and rows.
 */
public class PropertyEvaluatorSelectForge implements PropertyEvaluatorForge {
    private final SelectExprProcessorDescriptor selectExprProcessor;
    private final PropertyEvaluatorAccumulativeForge accumulative;

    public PropertyEvaluatorSelectForge(SelectExprProcessorDescriptor selectExprProcessor, PropertyEvaluatorAccumulativeForge accumulative) {
        this.selectExprProcessor = selectExprProcessor;
        this.accumulative = accumulative;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PropertyEvaluatorSelect.class, this.getClass(), classScope);
        CodegenExpressionNewAnonymousClass processor = SelectExprProcessorUtil.makeAnonymous(selectExprProcessor.getForge(), method, symbols.getAddInitSvc(method), classScope);
        method.getBlock()
                .declareVar(PropertyEvaluatorSelect.class, "pe", newInstance(PropertyEvaluatorSelect.class))
                .exprDotMethod(ref("pe"), "setResultEventType", EventTypeUtility.resolveTypeCodegen(selectExprProcessor.getForge().getResultEventType(), symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("pe"), "setAccumulative", accumulative.make(method, symbols, classScope))
                .exprDotMethod(ref("pe"), "setSelectExprProcessor", processor)
                .methodReturn(ref("pe"));
        return localMethod(method);
    }

    public EventType getFragmentEventType() {
        return selectExprProcessor.getForge().getResultEventType();
    }

    public boolean compareTo(PropertyEvaluatorForge other) {
        return false;
    }
}