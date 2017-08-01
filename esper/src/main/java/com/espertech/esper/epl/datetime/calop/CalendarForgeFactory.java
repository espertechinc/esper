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
import com.espertech.esper.epl.datetime.eval.ForgeFactory;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;

import java.util.List;

public class CalendarForgeFactory implements ForgeFactory {

    public CalendarForge getOp(DatetimeMethodEnum method, String methodNameUsed, List<ExprNode> parameters, ExprForge[] forges)
            throws ExprValidationException {
        if (method == DatetimeMethodEnum.WITHTIME) {
            return new CalendarWithTimeForge(forges[0], forges[1], forges[2], forges[3]);
        }
        if (method == DatetimeMethodEnum.WITHDATE) {
            return new CalendarWithDateForge(forges[0], forges[1], forges[2]);
        }
        if (method == DatetimeMethodEnum.PLUS || method == DatetimeMethodEnum.MINUS) {
            return new CalendarPlusMinusForge(forges[0], method == DatetimeMethodEnum.MINUS ? -1 : 1);
        }
        if (method == DatetimeMethodEnum.WITHMAX ||
                method == DatetimeMethodEnum.WITHMIN ||
                method == DatetimeMethodEnum.ROUNDCEILING ||
                method == DatetimeMethodEnum.ROUNDFLOOR ||
                method == DatetimeMethodEnum.ROUNDHALF ||
                method == DatetimeMethodEnum.SET) {
            CalendarFieldEnum fieldNum = CalendarOpUtil.getEnum(methodNameUsed, parameters.get(0));
            if (method == DatetimeMethodEnum.WITHMIN) {
                return new CalendarWithMinForge(fieldNum);
            }
            if (method == DatetimeMethodEnum.ROUNDCEILING || method == DatetimeMethodEnum.ROUNDFLOOR || method == DatetimeMethodEnum.ROUNDHALF) {
                return new CalendarForgeRound(fieldNum, method);
            } else if (method == DatetimeMethodEnum.SET) {
                return new CalendarSetForge(fieldNum, forges[1]);
            }
            return new CalendarWithMaxForge(fieldNum);
        }
        throw new IllegalStateException("Unrecognized calendar-op code '" + method + "'");
    }
}
