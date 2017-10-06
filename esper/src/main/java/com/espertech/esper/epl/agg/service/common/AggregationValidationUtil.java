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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Locale;

public class AggregationValidationUtil {
    public static void validateAggregationType(AggregationMethodFactory requiredFactory,
                                               AggregationMethodFactory providedFactory) throws ExprValidationException {
        if (!JavaClassHelper.isSubclassOrImplementsInterface(providedFactory.getClass(), requiredFactory.getClass())) {
            throw new ExprValidationException("Not a '" + requiredFactory.getAggregationExpression().getAggregationFunctionName() + "' aggregation");
        }
        ExprAggregateNode aggNodeRequired = requiredFactory.getAggregationExpression();
        ExprAggregateNode aggNodeProvided = providedFactory.getAggregationExpression();
        if (aggNodeRequired.isDistinct() != aggNodeProvided.isDistinct()) {
            throw new ExprValidationException("The aggregation declares " +
                    (aggNodeRequired.isDistinct() ? "a" : "no") +
                    " distinct and provided is " +
                    (aggNodeProvided.isDistinct() ? "a" : "no") +
                    " distinct");
        }
    }

    public static void validateAggregationInputType(Class requiredParam,
                                                    Class providedParam) throws ExprValidationException {
        Class boxedRequired = JavaClassHelper.getBoxedType(requiredParam);
        Class boxedProvided = JavaClassHelper.getBoxedType(providedParam);
        if (boxedRequired != boxedProvided &&
                !JavaClassHelper.isSubclassOrImplementsInterface(boxedProvided, boxedRequired)) {
            throw new ExprValidationException("The required parameter type is " +
                    JavaClassHelper.getClassNameFullyQualPretty(requiredParam) +
                    " and provided is " +
                    JavaClassHelper.getClassNameFullyQualPretty(providedParam));
        }
    }

    public static void validateAggregationFilter(boolean requireFilter,
                                                 boolean provideFilter) throws ExprValidationException {
        if (requireFilter != provideFilter) {
            throw new ExprValidationException("The aggregation declares " +
                    (requireFilter ? "a" : "no") +
                    " filter expression and provided is " +
                    (provideFilter ? "a" : "no") +
                    " filter expression");
        }
    }

    public static void validateAggregationUnbound(boolean requiredHasDataWindows, boolean providedHasDataWindows)
            throws ExprValidationException {
        if (requiredHasDataWindows != providedHasDataWindows) {
            throw new ExprValidationException("The aggregation declares " +
                    (requiredHasDataWindows ? "use with data windows" : "unbound") +
                    " and provided is " +
                    (providedHasDataWindows ? "use with data windows" : "unbound"));
        }
    }

    public static void validateEventType(EventType requiredType, EventType providedType)
            throws ExprValidationException {
        if (!EventTypeUtility.isTypeOrSubTypeOf(providedType, requiredType)) {
            throw new ExprValidationException("The required event type is '" +
                    requiredType.getName() +
                    "' and provided is '" + providedType.getName() + "'");
        }
    }

    public static void validateAggFuncName(String requiredName, String providedName)
            throws ExprValidationException {
        if (!requiredName.toLowerCase(Locale.ENGLISH).equals(providedName)) {
            throw new ExprValidationException("The required aggregation function name is '" +
                    requiredName + "' and provided is '" + providedName + "'");
        }
    }

    public static void validateStreamNumZero(int streamNum) throws ExprValidationException {
        if (streamNum != 0) {
            throw new ExprValidationException("The from-clause order requires the stream in position zero");
        }
    }
}