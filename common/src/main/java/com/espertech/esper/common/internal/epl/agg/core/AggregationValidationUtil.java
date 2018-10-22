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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Locale;

public class AggregationValidationUtil {

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
            throw new ExprValidationException("The table declares " +
                    (requiredHasDataWindows ? "use with data windows" : "unbound") +
                    " and provided is " +
                    (providedHasDataWindows ? "use with data windows" : "unbound"));
        }
    }

    public static void validateAggregationType(AggregationPortableValidation tableDeclared, String tableExpression, AggregationPortableValidation intoTableDeclared, String intoExpression) throws ExprValidationException {
        if (tableDeclared.getClass() != intoTableDeclared.getClass()) {
            throw new ExprValidationException("The table declares '" +
                    tableExpression +
                    "' and provided is '" +
                    intoExpression + "'");
        }
    }

    public static void validateAggFuncName(String requiredName, String providedName)
            throws ExprValidationException {
        if (!requiredName.toLowerCase(Locale.ENGLISH).equals(providedName)) {
            throw new ExprValidationException("The required aggregation function name is '" +
                    requiredName + "' and provided is '" + providedName + "'");
        }
    }

    public static void validateDistinct(boolean required, boolean provided) throws ExprValidationException {
        if (required != provided) {
            throw new ExprValidationException("The aggregation declares " +
                    (required ? "a" : "no") +
                    " distinct and provided is " +
                    (provided ? "a" : "no") +
                    " distinct");
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
}