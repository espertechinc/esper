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
package com.espertech.esper.epl.datetime.calop;

import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.OpFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;

import java.util.List;

public class CalendarOpFactory implements OpFactory {

    public CalendarOp getOp(DatetimeMethodEnum method, String methodNameUsed, List<ExprNode> parameters, ExprEvaluator[] evaluators)
            throws ExprValidationException {
        if (method == DatetimeMethodEnum.WITHTIME) {
            return new CalendarOpWithTime(evaluators[0], evaluators[1], evaluators[2], evaluators[3]);
        }
        if (method == DatetimeMethodEnum.WITHDATE) {
            return new CalendarOpWithDate(evaluators[0], evaluators[1], evaluators[2]);
        }
        if (method == DatetimeMethodEnum.PLUS || method == DatetimeMethodEnum.MINUS) {
            return new CalendarOpPlusMinus(evaluators[0], method == DatetimeMethodEnum.MINUS ? -1 : 1);
        }
        if (method == DatetimeMethodEnum.WITHMAX ||
                method == DatetimeMethodEnum.WITHMIN ||
                method == DatetimeMethodEnum.ROUNDCEILING ||
                method == DatetimeMethodEnum.ROUNDFLOOR ||
                method == DatetimeMethodEnum.ROUNDHALF ||
                method == DatetimeMethodEnum.SET) {
            CalendarFieldEnum fieldNum = CalendarOpUtil.getEnum(methodNameUsed, parameters.get(0));
            if (method == DatetimeMethodEnum.WITHMIN) {
                return new CalendarOpWithMin(fieldNum);
            }
            if (method == DatetimeMethodEnum.ROUNDCEILING || method == DatetimeMethodEnum.ROUNDFLOOR || method == DatetimeMethodEnum.ROUNDHALF) {
                return new CalendarOpRound(fieldNum, method);
            } else if (method == DatetimeMethodEnum.SET) {
                return new CalendarOpSet(fieldNum, evaluators[1]);
            }
            return new CalendarOpWithMax(fieldNum);
        }
        throw new IllegalStateException("Unrecognized calendar-op code '" + method + "'");
    }
}
