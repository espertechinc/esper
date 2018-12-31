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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;
import com.espertech.esper.common.internal.schedule.ScheduleSpecUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for 'crontab' observers that indicate truth when a time point was reached.
 */
public class TimerAtObserverForge implements ObserverForge, ScheduleHandleCallbackProvider {
    private static final Logger log = LoggerFactory.getLogger(TimerAtObserverForge.class);

    private List<ExprNode> parameters;
    private MatchedEventConvertorForge convertor;
    private ScheduleSpec spec = null;
    private int scheduleCallbackId = -1;

    public void setObserverParameters(List<ExprNode> parameters, MatchedEventConvertorForge convertor, ExprValidationContext validationContext) throws ObserverParameterException {
        ObserverParameterUtil.validateNoNamedParameters("timer:at", parameters);
        if (log.isDebugEnabled()) {
            log.debug(".setObserverParameters " + parameters);
        }

        if ((parameters.size() < 5) || (parameters.size() > 9)) {
            throw new ObserverParameterException("Invalid number of parameters for timer:at");
        }

        this.parameters = parameters;
        this.convertor = convertor;

        // if all parameters are constants, lets try to evaluate and build a schedule for early validation
        boolean allConstantResult = true;
        for (ExprNode param : parameters) {
            if ((!(param instanceof ExprWildcard)) && !param.getForge().getForgeConstantType().isCompileTimeConstant()) {
                allConstantResult = false;
            }
        }

        if (allConstantResult) {
            try {
                List<Object> observerParameters = evaluateCompileTime(parameters);
                spec = ScheduleSpecUtil.computeValues(observerParameters.toArray());
            } catch (ScheduleParameterException e) {
                throw new ObserverParameterException("Error computing crontab schedule specification: " + e.getMessage(), e);
            }
        }
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(TimerAtObserverFactory.class, TimerIntervalObserverForge.class, classScope);

        CodegenExpression parametersExpr;
        CodegenExpression optionalConvertorExpr;
        CodegenExpression specExpr;
        if (spec != null) { // handle all-constant specification
            parametersExpr = constantNull();
            optionalConvertorExpr = constantNull();
            specExpr = spec.make(method, classScope);
        } else {
            specExpr = constantNull();
            optionalConvertorExpr = convertor.makeAnonymous(method, classScope);
            parametersExpr = ExprNodeUtilityCodegen.codegenEvaluators(ExprNodeUtilityQuery.toArray(parameters), method, this.getClass(), classScope);
        }

        method.getBlock()
                .declareVar(TimerAtObserverFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add("observerTimerAt"))
                .exprDotMethod(ref("factory"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("factory"), "setParameters", parametersExpr)
                .exprDotMethod(ref("factory"), "setOptionalConvertor", optionalConvertorExpr)
                .exprDotMethod(ref("factory"), "setSpec", specExpr)
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    public void collectSchedule(List<ScheduleHandleCallbackProvider> schedules) {
        schedules.add(this);
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    private static List<Object> evaluateCompileTime(List<ExprNode> parameters)
            throws EPException {
        List<Object> results = new ArrayList<>();
        int count = 0;
        for (ExprNode expr : parameters) {
            try {
                Object result = expr.getForge().getExprEvaluator().evaluate(null, true, null);
                results.add(result);
                count++;
            } catch (RuntimeException ex) {
                String message = "Tmer-at observer invalid parameter in expression " + count;
                if (ex.getMessage() != null) {
                    message += ": " + ex.getMessage();
                }
                log.error(message, ex);
                throw new EPException(message);
            }
        }
        return results;
    }
}
