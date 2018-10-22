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
package com.espertech.esper.common.internal.view.time_accum;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewFactoryTimePeriodHelper;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class TimeAccumViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, ScheduleHandleCallbackProvider {
    private int scheduleCallbackId = -1;
    protected TimePeriodComputeForge timePeriodCompute;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        if (parameters.size() != 1) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timePeriodCompute = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), parameters.get(0), getViewParamMessage(), 0, viewForgeEnv, streamNumber);
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return TimeAccumViewFactory.class;
    }

    protected String factoryMethod() {
        return "timeaccum";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("No schedule callback id");
        }
        method.getBlock()
                .declareVar(TimePeriodCompute.class, "eval", timePeriodCompute.makeEvaluator(method, classScope))
                .exprDotMethod(factory, "setTimePeriodCompute", ref("eval"))
                .exprDotMethod(factory, "setScheduleCallbackId", constant(scheduleCallbackId));
    }

    public String getViewName() {
        return "Time-Accumulative-Batch";
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a single numeric parameter or time period parameter";
    }
}
