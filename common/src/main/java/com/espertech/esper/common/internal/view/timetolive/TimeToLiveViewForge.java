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
package com.espertech.esper.common.internal.view.timetolive;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeConstGivenDeltaForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluator;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeLong;

public class TimeToLiveViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, ScheduleHandleCallbackProvider {
    private List<ExprNode> viewParameters;

    protected ExprNode timestampExpression;
    private int scheduleCallbackId = -1;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        viewParameters = parameters;
    }

    public void attachValidate(EventType parentEventType, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        ExprNode[] validated = ViewForgeSupport.validate(getViewName(), parentEventType, viewParameters, true, viewForgeEnv);

        if (viewParameters.size() != 1) {
            throw new ViewParameterException(getViewParamMessage());
        }
        if (!isTypeLong(validated[0].getForge().getEvaluationType())) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timestampExpression = validated[0];
        eventType = parentEventType;
    }

    protected EPTypeClass typeOfFactory() {
        return TimeOrderViewFactory.EPTYPE;
    }

    protected String factoryMethod() {
        return "timeorder";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
            .declareVar(TimePeriodCompute.EPTYPE, "eval", new TimePeriodComputeConstGivenDeltaForge(0).makeEvaluator(method, classScope))
            .exprDotMethod(factory, "setTimestampEval", codegenEvaluator(timestampExpression.getForge(), method, TimeOrderViewForge.class, classScope))
            .exprDotMethod(factory, "setTimePeriodCompute", ref("eval"))
            .exprDotMethod(factory, "setScheduleCallbackId", constant(scheduleCallbackId))
            .exprDotMethod(factory, "setTimeToLive", constantTrue());
    }

    public String getViewName() {
        return "Time-To-Live";
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a single expression supplying long-type timestamp values as a parameter";
    }

    public AppliesTo appliesTo() {
        return AppliesTo.WINDOW_TIMETOLIVE;
    }

    public <T> T accept(ViewFactoryForgeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
