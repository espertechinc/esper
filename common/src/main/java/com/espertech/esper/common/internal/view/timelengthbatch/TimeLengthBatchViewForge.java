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
package com.espertech.esper.common.internal.view.timelengthbatch;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.TimeBatchFlags;
import com.espertech.esper.common.internal.view.util.ViewFactoryTimePeriodHelper;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluator;

public class TimeLengthBatchViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, ScheduleHandleCallbackProvider, DataWindowBatchingViewForge {
    /**
     * Number of events to collect before batch fires.
     */
    protected ExprForge sizeForge;
    protected boolean isForceUpdate;
    protected boolean isStartEager;
    protected TimePeriodComputeForge timePeriodCompute;
    protected int scheduleCallbackId;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        ExprNode[] validated = ViewForgeSupport.validate(getViewName(), parameters, viewForgeEnv, streamNumber);
        String errorMessage = getViewName() + " view requires a numeric or time period parameter as a time interval size, and an integer parameter as a maximal number-of-events, and an optional list of control keywords as a string parameter (please see the documentation)";
        if ((validated.length != 2) && (validated.length != 3)) {
            throw new ViewParameterException(errorMessage);
        }

        timePeriodCompute = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), parameters.get(0), errorMessage, 0, viewForgeEnv, streamNumber);

        sizeForge = ViewForgeSupport.validateSizeParam(getViewName(), validated[1], 1);

        if (validated.length > 2) {
            Object keywords = ViewForgeSupport.evaluate(validated[2].getForge().getExprEvaluator(), 2, getViewName());
            TimeBatchFlags flags = TimeBatchFlags.processKeywords(keywords, errorMessage);
            this.isForceUpdate = flags.isForceUpdate();
            this.isStartEager = flags.isStartEager();
        }
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    protected Class typeOfFactory() {
        return TimeLengthBatchViewFactory.class;
    }

    protected String factoryMethod() {
        return "timelengthbatch";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("No schedule callback id");
        }
        method.getBlock()
                .declareVar(TimePeriodCompute.class, "eval", timePeriodCompute.makeEvaluator(method, classScope))
                .exprDotMethod(factory, "setSize", codegenEvaluator(sizeForge, method, this.getClass(), classScope))
                .exprDotMethod(factory, "setTimePeriodCompute", ref("eval"))
                .exprDotMethod(factory, "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(factory, "setForceUpdate", constant(isForceUpdate))
                .exprDotMethod(factory, "setStartEager", constant(isStartEager));
    }

    public String getViewName() {
        return "Time-Length-Batch";
    }
}
