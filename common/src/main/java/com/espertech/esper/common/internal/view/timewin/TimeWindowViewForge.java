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
package com.espertech.esper.common.internal.view.timewin;

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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimeWindowViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, ScheduleHandleCallbackProvider {
    protected TimePeriodComputeForge timePeriodComputeForge;
    private int scheduleCallbackId = -1;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        if (parameters.size() != 1) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timePeriodComputeForge = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), parameters.get(0), getViewParamMessage(), 0, viewForgeEnv, streamNumber);
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return TimeWindowViewFactory.class;
    }

    protected String factoryMethod() {
        return "time";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("No schedule callback id");
        }
        method.getBlock()
                .declareVar(TimePeriodCompute.class, "eval", timePeriodComputeForge.makeEvaluator(method, classScope))
                .exprDotMethod(factory, "setTimePeriodCompute", ref("eval"))
                .exprDotMethod(factory, "setScheduleCallbackId", constant(scheduleCallbackId));
    }

    public String getViewName() {
        return "Time";
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a single numeric or time period parameter";
    }
}
