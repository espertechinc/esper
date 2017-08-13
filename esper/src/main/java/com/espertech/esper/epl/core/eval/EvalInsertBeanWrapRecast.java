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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.WrapperEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertBeanWrapRecast implements SelectExprProcessor, SelectExprProcessorForge {

    private final WrapperEventType eventType;
    private final EventAdapterService eventAdapterService;
    private final int streamNumber;

    public EvalInsertBeanWrapRecast(WrapperEventType targetType, EventAdapterService eventAdapterService, int streamNumber, EventType[] typesPerStream)
            throws ExprValidationException {
        this.eventType = targetType;
        this.eventAdapterService = eventAdapterService;
        this.streamNumber = streamNumber;

        EventType sourceType = typesPerStream[streamNumber];
        Class sourceClass = sourceType.getUnderlyingType();
        Class targetClass = targetType.getUnderlyingEventType().getUnderlyingType();
        if (!JavaClassHelper.isSubclassOrImplementsInterface(sourceClass, targetClass)) {
            throw EvalInsertUtil.makeEventTypeCastException(sourceType, targetType);
        }
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamNumber];
        EventBean recast = eventAdapterService.adapterForTypedBean(theEvent.getUnderlying(), eventType.getUnderlyingEventType());
        return eventAdapterService.adapterForTypedWrapper(recast, Collections.<String, Object>emptyMap(), eventType);
    }

    public EventType getResultEventType() {
        return eventType;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        return this;
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenMember memberUnderlyingType = context.makeAddMember(EventType.class, eventType.getUnderlyingEventType());
        CodegenMethodId method = context.addMethod(EventBean.class, this.getClass()).add(params).begin()
                .declareVar(EventBean.class, "theEvent", arrayAtIndex(params.passEPS(), constant(streamNumber)))
                .declareVar(EventBean.class, "recast", exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedBean", exprDotUnderlying(ref("theEvent")), member(memberUnderlyingType.getMemberId())))
                .methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", ref("recast"), staticMethod(Collections.class, "emptyMap"), member(memberResultEventType.getMemberId())));
        return localMethodBuild(method).passAll(params).call();
    }
}
