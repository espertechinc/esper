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
package com.espertech.esper.common.internal.view.timebatch;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.TimeBatchFlags;
import com.espertech.esper.common.internal.view.util.ViewFactoryTimePeriodHelper;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

/**
 * Factory for {@link TimeBatchView}.
 */
public class TimeBatchViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, ScheduleHandleCallbackProvider, DataWindowBatchingViewForge {

    /**
     * The reference point, or null if none supplied.
     */
    protected Long optionalReferencePoint;
    protected boolean isForceUpdate;
    protected boolean isStartEager;
    protected TimePeriodComputeForge timePeriodCompute;
    protected int scheduleCallbackId;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        if ((parameters.size() < 1) || (parameters.size() > 3)) {
            throw new ViewParameterException(getViewParamMessage());
        }
        Object[] viewParamValues = new Object[parameters.size()];
        for (int i = 1; i < viewParamValues.length; i++) {
            viewParamValues[i] = ViewForgeSupport.validateAndEvaluate(getViewName(), parameters.get(i), viewForgeEnv, streamNumber);
        }

        timePeriodCompute = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), parameters.get(0), getViewParamMessage(), 0, viewForgeEnv, streamNumber);

        TimeBatchFlags timeBatchFlags = new TimeBatchFlags(false, false);
        if ((viewParamValues.length == 2) && (viewParamValues[1] instanceof String)) {
            timeBatchFlags = TimeBatchFlags.processKeywords(viewParamValues[1], getViewParamMessage());
        } else {
            if (viewParamValues.length >= 2) {
                Object paramRef = viewParamValues[1];
                if ((!(paramRef instanceof Number)) || (JavaClassHelper.isFloatingPointNumber((Number) paramRef))) {
                    throw new ViewParameterException(getViewName() + " view requires a Long-typed reference point in msec as a second parameter");
                }
                optionalReferencePoint = ((Number) paramRef).longValue();
            }
            if (viewParamValues.length == 3) {
                timeBatchFlags = TimeBatchFlags.processKeywords(viewParamValues[2], getViewParamMessage());
            }
        }
        this.isForceUpdate = timeBatchFlags.isForceUpdate();
        this.isStartEager = timeBatchFlags.isStartEager();
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return TimeBatchViewFactory.class;
    }

    protected String factoryMethod() {
        return "timebatch";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("No schedule callback id");
        }
        method.getBlock()
                .declareVar(TimePeriodCompute.class, "eval", timePeriodCompute.makeEvaluator(method, classScope))
                .exprDotMethod(factory, "setTimePeriodCompute", ref("eval"))
                .exprDotMethod(factory, "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(factory, "setForceUpdate", constant(isForceUpdate))
                .exprDotMethod(factory, "setStartEager", constant(isStartEager))
                .exprDotMethod(factory, "setOptionalReferencePoint", constant(optionalReferencePoint));
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a single numeric or time period parameter, and an optional long-typed reference point in msec, and an optional list of control keywords as a string parameter (please see the documentation)";
    }

    public String getViewName() {
        return "Time-Batch";
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }
}
