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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.datetime.calop.CalendarOpFactory;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.datetime.interval.IntervalOpFactory;
import com.espertech.esper.epl.datetime.reformatop.ReformatOp;
import com.espertech.esper.epl.datetime.reformatop.ReformatOpFactory;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.methodbase.DotMethodFPProvided;
import com.espertech.esper.epl.methodbase.DotMethodInputTypeMatcher;
import com.espertech.esper.epl.methodbase.DotMethodTypeEnum;
import com.espertech.esper.epl.methodbase.DotMethodUtil;
import com.espertech.esper.epl.rettype.*;
import com.espertech.esper.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TimeZone;

public class ExprDotEvalDTFactory {

    public static ExprDotEvalDTMethodDesc validateMake(StreamTypeService streamTypeService, Deque<ExprChainedSpec> chainSpecStack, DatetimeMethodEnum dtMethod, String dtMethodName, EPType inputType, List<ExprNode> parameters, ExprDotNodeFilterAnalyzerInput inputDesc, TimeZone timeZone, TimeAbacus timeAbacus, ExprEvaluatorContext exprEvaluatorContext)
            throws ExprValidationException {
        // verify input
        String message = "Date-time enumeration method '" + dtMethodName + "' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property";
        if (inputType instanceof EventEPType) {
            if (((EventEPType) inputType).getType().getStartTimestampPropertyName() == null) {
                throw new ExprValidationException(message);
            }
        } else {
            if (!(inputType instanceof ClassEPType || inputType instanceof NullEPType)) {
                throw new ExprValidationException(message + " but received " + EPTypeHelper.toTypeDescriptive(inputType));
            }
            if (inputType instanceof ClassEPType) {
                ClassEPType classEPType = (ClassEPType) inputType;
                if (!JavaClassHelper.isDatetimeClass(classEPType.getType())) {
                    throw new ExprValidationException(message + " but received " + JavaClassHelper.getClassNameFullyQualPretty(classEPType.getType()));
                }
            }
        }

        List<CalendarOp> calendarOps = new ArrayList<CalendarOp>();
        ReformatOp reformatOp = null;
        IntervalOp intervalOp = null;
        DatetimeMethodEnum currentMethod = dtMethod;
        List<ExprNode> currentParameters = parameters;
        String currentMethodName = dtMethodName;

        // drain all calendar ops
        ExprDotNodeFilterAnalyzerDesc filterAnalyzerDesc = null;
        while (true) {

            // handle the first one only if its a calendar op
            ExprEvaluator[] evaluators = getEvaluators(currentParameters);
            OpFactory opFactory = currentMethod.getOpFactory();

            // compile parameter abstract for validation against available footprints
            DotMethodFPProvided footprintProvided = DotMethodUtil.getProvidedFootprint(currentParameters);

            // validate parameters
            DotMethodUtil.validateParametersDetermineFootprint(currentMethod.getFootprints(), DotMethodTypeEnum.DATETIME, currentMethodName, footprintProvided, DotMethodInputTypeMatcher.DEFAULT_ALL);

            if (opFactory instanceof CalendarOpFactory) {
                CalendarOp calendarOp = ((CalendarOpFactory) currentMethod.getOpFactory()).getOp(currentMethod, currentMethodName, currentParameters, evaluators);
                calendarOps.add(calendarOp);
            } else if (opFactory instanceof ReformatOpFactory) {
                reformatOp = ((ReformatOpFactory) opFactory).getOp(inputType, timeZone, timeAbacus, currentMethod, currentMethodName, currentParameters, exprEvaluatorContext);

                // compile filter analyzer information if there are no calendar ops in the chain
                if (calendarOps.isEmpty()) {
                    filterAnalyzerDesc = reformatOp.getFilterDesc(streamTypeService.getEventTypes(), currentMethod, currentParameters, inputDesc);
                } else {
                    filterAnalyzerDesc = null;
                }
            } else if (opFactory instanceof IntervalOpFactory) {
                intervalOp = ((IntervalOpFactory) opFactory).getOp(streamTypeService, currentMethod, currentMethodName, currentParameters, timeZone, timeAbacus);

                // compile filter analyzer information if there are no calendar ops in the chain
                if (calendarOps.isEmpty()) {
                    filterAnalyzerDesc = intervalOp.getFilterDesc(streamTypeService.getEventTypes(), currentMethod, currentParameters, inputDesc);
                } else {
                    filterAnalyzerDesc = null;
                }
            } else {
                throw new IllegalStateException("Invalid op factory class " + opFactory);
            }

            // see if there is more
            if (chainSpecStack.isEmpty() || !DatetimeMethodEnum.isDateTimeMethod(chainSpecStack.getFirst().getName())) {
                break;
            }

            // pull next
            ExprChainedSpec next = chainSpecStack.removeFirst();
            currentMethod = DatetimeMethodEnum.fromName(next.getName());
            currentParameters = next.getParameters();
            currentMethodName = next.getName();

            if (reformatOp != null || intervalOp != null) {
                throw new ExprValidationException("Invalid input for date-time method '" + next.getName() + "'");
            }
        }

        ExprDotEval dotEval;
        EPType returnType;

        dotEval = new ExprDotEvalDT(calendarOps, timeZone, timeAbacus, reformatOp, intervalOp, EPTypeHelper.getClassSingleValued(inputType), EPTypeHelper.getEventTypeSingleValued(inputType));
        returnType = dotEval.getTypeInfo();
        return new ExprDotEvalDTMethodDesc(dotEval, returnType, filterAnalyzerDesc);
    }

    private static ExprEvaluator[] getEvaluators(List<ExprNode> parameters) {

        ExprEvaluator[] inputExpr = new ExprEvaluator[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {

            ExprNode innerExpr = parameters.get(i);
            final ExprEvaluator inner = innerExpr.getExprEvaluator();

            // Time periods get special attention
            if (innerExpr instanceof ExprTimePeriod) {

                final ExprTimePeriod timePeriod = (ExprTimePeriod) innerExpr;
                inputExpr[i] = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                        return timePeriod.evaluateGetTimePeriod(eventsPerStream, isNewData, context);
                    }

                    public Class getType() {
                        return TimePeriod.class;
                    }
                };
            } else {
                inputExpr[i] = inner;
            }
        }
        return inputExpr;
    }
}
