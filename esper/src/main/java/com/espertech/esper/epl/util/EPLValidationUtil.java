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
package com.espertech.esper.epl.util;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprStreamUnderlyingNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.util.JavaClassHelper;

public class EPLValidationUtil {

    public static void validateParameterNumber(String invocableName, String invocableCategory, boolean isFunction, int expectedEnum, int receivedNum) throws ExprValidationException {
        if (expectedEnum != receivedNum) {
            throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected " + expectedEnum + " parameters but received " + receivedNum + " parameters");
        }
    }

    public static void validateParameterType(String invocableName, String invocableCategory, boolean isFunction, EPLExpressionParamType expectedTypeEnum, Class[] expectedTypeClasses, Class providedType, int parameterNum, ExprNode parameterExpression)
            throws ExprValidationException {
        if (expectedTypeEnum == EPLExpressionParamType.BOOLEAN && (!JavaClassHelper.isBoolean(providedType))) {
            throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a boolean-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
        }
        if (expectedTypeEnum == EPLExpressionParamType.NUMERIC && (!JavaClassHelper.isNumeric(providedType))) {
            throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a number-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
        }
        if (expectedTypeEnum == EPLExpressionParamType.SPECIFIC) {
            Class boxedProvidedType = JavaClassHelper.getBoxedType(providedType);
            boolean found = false;
            for (Class expectedTypeClass : expectedTypeClasses) {
                Class boxedExpectedType = JavaClassHelper.getBoxedType(expectedTypeClass);
                if (boxedProvidedType != null && JavaClassHelper.isSubclassOrImplementsInterface(boxedProvidedType, boxedExpectedType)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String expected;
                if (expectedTypeClasses.length == 1) {
                    expected = "a " + JavaClassHelper.getParameterAsString(expectedTypeClasses);
                } else {
                    expected = "any of [" + JavaClassHelper.getParameterAsString(expectedTypeClasses) + "]";
                }
                throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected " + expected + "-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
        if (expectedTypeEnum == EPLExpressionParamType.TIME_PERIOD_OR_SEC) {
            if (parameterExpression instanceof ExprTimePeriod || parameterExpression instanceof ExprStreamUnderlyingNode) {
                return;
            }
            if (!(JavaClassHelper.isNumeric(providedType))) {
                throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a time-period expression or a numeric-type result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
        if (expectedTypeEnum == EPLExpressionParamType.DATETIME) {
            if (!(JavaClassHelper.isDatetimeClass(providedType))) {
                throw new ExprValidationException(getInvokablePrefix(invocableName, invocableCategory, isFunction) + "expected a long-typed, Date-typed or Calendar-typed result for expression parameter " + parameterNum + " but received " + JavaClassHelper.getClassNameFullyQualPretty(providedType));
            }
        }
    }

    public static void validateTableExists(TableService tableService, String name) throws ExprValidationException {
        if (tableService.getTableMetadata(name) != null) {
            throw new ExprValidationException("A table by name '" + name + "' already exists");
        }
    }

    public static void validateContextName(boolean table, String tableOrNamedWindowName, String tableOrNamedWindowContextName, String optionalContextName, boolean mustMatchContext)
            throws ExprValidationException {
        if (tableOrNamedWindowContextName != null) {
            if (optionalContextName == null || !optionalContextName.equals(tableOrNamedWindowContextName)) {
                throw getCtxMessage(table, tableOrNamedWindowName, tableOrNamedWindowContextName);
            }
        } else {
            if (mustMatchContext && optionalContextName != null) {
                throw getCtxMessage(table, tableOrNamedWindowName, tableOrNamedWindowContextName);
            }
        }
    }

    private static ExprValidationException getCtxMessage(boolean table, String tableOrNamedWindowName, String tableOrNamedWindowContextName) {
        String prefix = table ? "Table" : "Named window";
        return new ExprValidationException(prefix + " by name '" + tableOrNamedWindowName + "' has been declared for context '" + tableOrNamedWindowContextName + "' and can only be used within the same context");
    }

    public static String getInvokablePrefix(String invocableName, String invocableType, boolean isFunction) {
        return "Error validating " + invocableType + " " + (isFunction ? "function '" : "method '") + invocableName + "', ";
    }

    public static void validateParametersTypePredefined(ExprNode[] expressions, String invocableName, String invocableCategory, EPLExpressionParamType type) throws ExprValidationException {
        for (int i = 0; i < expressions.length; i++) {
            EPLValidationUtil.validateParameterType(invocableName, invocableCategory, true, type, null, expressions[i].getForge().getEvaluationType(), i, expressions[i]);
        }
    }
}
