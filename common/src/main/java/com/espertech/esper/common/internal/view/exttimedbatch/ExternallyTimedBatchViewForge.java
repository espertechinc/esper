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
package com.espertech.esper.common.internal.view.exttimedbatch;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewFactoryTimePeriodHelper;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluator;

public class ExternallyTimedBatchViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious, DataWindowBatchingViewForge {
    private List<ExprNode> viewParameters;
    private ExprNode timestampExpression;
    private Long optionalReferencePoint;
    private TimePeriodComputeForge timePeriodComputeForge;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        final String windowName = getViewName();
        ExprNode[] validated = ViewForgeSupport.validate(windowName, parentEventType, viewParameters, true, viewForgeEnv, streamNumber);
        if (viewParameters.size() < 2 || viewParameters.size() > 3) {
            throw new ViewParameterException(getViewParamMessage());
        }

        // validate first parameter: timestamp expression
        if (!JavaClassHelper.isNumeric(validated[0].getForge().getEvaluationType())) {
            throw new ViewParameterException(getViewParamMessage());
        }
        timestampExpression = validated[0];
        ViewForgeSupport.assertReturnsNonConstant(windowName, validated[0], 0);

        timePeriodComputeForge = ViewFactoryTimePeriodHelper.validateAndEvaluateTimeDeltaFactory(getViewName(), viewParameters.get(1), getViewParamMessage(), 1, viewForgeEnv, streamNumber);

        // validate optional parameters
        if (validated.length == 3) {
            Object constant = ViewForgeSupport.validateAndEvaluate(windowName, validated[2], viewForgeEnv, streamNumber);
            if ((!(constant instanceof Number)) || (JavaClassHelper.isFloatingPointNumber((Number) constant))) {
                throw new ViewParameterException("Externally-timed batch view requires a Long-typed reference point in msec as a third parameter");
            }
            optionalReferencePoint = ((Number) constant).longValue();
        }

        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return ExternallyTimedBatchViewFactory.class;
    }

    protected String factoryMethod() {
        return "exttimebatch";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .declareVar(TimePeriodCompute.class, "eval", timePeriodComputeForge.makeEvaluator(method, classScope))
                .exprDotMethod(factory, "setTimePeriodCompute", ref("eval"))
                .exprDotMethod(factory, "setOptionalReferencePoint", constant(optionalReferencePoint))
                .exprDotMethod(factory, "setTimestampEval", codegenEvaluator(timestampExpression.getForge(), method, this.getClass(), classScope));
    }

    public String getViewName() {
        return "Externally-timed-batch";
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires a timestamp expression and a numeric or time period parameter for window size and an optional long-typed reference point in msec, and an optional list of control keywords as a string parameter (please see the documentation)";
    }
}
