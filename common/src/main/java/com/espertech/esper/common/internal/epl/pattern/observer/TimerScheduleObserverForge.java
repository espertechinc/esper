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

import com.espertech.esper.common.client.util.TimePeriod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for ISO8601 repeating interval observers that indicate truth when a time point was reached.
 */
public class TimerScheduleObserverForge implements ObserverForge, ScheduleHandleCallbackProvider {
    protected final static String NAME_OBSERVER = "Timer-schedule observer";

    private final static String ISO_NAME = "iso";
    private final static String REPETITIONS_NAME = "repetitions";
    private final static String DATE_NAME = "date";
    private final static String PERIOD_NAME = "period";
    private final static String[] NAMED_PARAMETERS = {ISO_NAME, REPETITIONS_NAME, DATE_NAME, PERIOD_NAME};

    private TimerScheduleSpecComputeForge scheduleComputer;
    private MatchedEventConvertorForge convertor;
    private boolean allConstantResult;
    private int scheduleCallbackId = -1;

    public void setObserverParameters(List<ExprNode> parameters, MatchedEventConvertorForge convertor, ExprValidationContext validationContext) throws ObserverParameterException {
        this.convertor = convertor;

        // obtains name parameters
        Map<String, ExprNamedParameterNode> namedExpressions;
        try {
            namedExpressions = ExprNodeUtilityValidate.getNamedExpressionsHandleDups(parameters);
            ExprNodeUtilityValidate.validateNamed(namedExpressions, NAMED_PARAMETERS);
        } catch (ExprValidationException e) {
            throw new ObserverParameterException(e.getMessage(), e);
        }

        ExprNamedParameterNode isoStringExpr = namedExpressions.get(ISO_NAME);
        if (namedExpressions.size() == 1 && isoStringExpr != null) {
            try {
                allConstantResult = ExprNodeUtilityValidate.validateNamedExpectType(isoStringExpr, new Class[]{String.class});
            } catch (ExprValidationException ex) {
                throw new ObserverParameterException(ex.getMessage(), ex);
            }
            scheduleComputer = new TimerScheduleSpecComputeISOStringForge(isoStringExpr.getChildNodes()[0]);
        } else if (isoStringExpr != null) {
            throw new ObserverParameterException("The '" + ISO_NAME + "' parameter is exclusive of other parameters");
        } else if (namedExpressions.size() == 0) {
            throw new ObserverParameterException("No parameters provided");
        } else {
            allConstantResult = true;
            ExprNamedParameterNode dateNamedNode = namedExpressions.get(DATE_NAME);
            ExprNamedParameterNode repetitionsNamedNode = namedExpressions.get(REPETITIONS_NAME);
            ExprNamedParameterNode periodNamedNode = namedExpressions.get(PERIOD_NAME);
            if (dateNamedNode == null && periodNamedNode == null) {
                throw new ObserverParameterException("Either the date or period parameter is required");
            }
            try {
                if (dateNamedNode != null) {
                    allConstantResult = ExprNodeUtilityValidate.validateNamedExpectType(dateNamedNode, new Class[]{String.class, Calendar.class, Date.class, Long.class, LocalDateTime.class, ZonedDateTime.class});
                }
                if (repetitionsNamedNode != null) {
                    allConstantResult &= ExprNodeUtilityValidate.validateNamedExpectType(repetitionsNamedNode, new Class[]{Integer.class, Long.class});
                }
                if (periodNamedNode != null) {
                    allConstantResult &= ExprNodeUtilityValidate.validateNamedExpectType(periodNamedNode, new Class[]{TimePeriod.class});
                }
            } catch (ExprValidationException ex) {
                throw new ObserverParameterException(ex.getMessage(), ex);
            }
            ExprNode dateNode = dateNamedNode == null ? null : dateNamedNode.getChildNodes()[0];
            ExprNode repetitionsNode = repetitionsNamedNode == null ? null : repetitionsNamedNode.getChildNodes()[0];
            ExprTimePeriod periodNode = periodNamedNode == null ? null : (ExprTimePeriod) periodNamedNode.getChildNodes()[0];
            scheduleComputer = new TimerScheduleSpecComputeFromExprForge(dateNode, repetitionsNode, periodNode);
        }

        if (allConstantResult) {
            try {
                scheduleComputer.verifyComputeAllConst(validationContext);
            } catch (ScheduleParameterException ex) {
                throw new ObserverParameterException(ex.getMessage(), ex);
            }
        }
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(TimerScheduleObserverFactory.class, TimerIntervalObserverForge.class, classScope);

        method.getBlock()
                .declareVar(TimerScheduleObserverFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add("observerTimerSchedule"))
                .exprDotMethod(ref("factory"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("factory"), "setAllConstant", constant(allConstantResult))
                .exprDotMethod(ref("factory"), "setScheduleComputer", scheduleComputer.make(method, classScope))
                .exprDotMethod(ref("factory"), "setOptionalConvertor", convertor == null ? null : convertor.makeAnonymous(method, classScope))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    public void collectSchedule(List<ScheduleHandleCallbackProvider> schedules) {
        schedules.add(this);
    }
}
