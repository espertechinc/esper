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
package com.espertech.esper.epl.index.service;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;

import static com.espertech.esper.util.JavaClassHelper.isNumeric;

public class AdvancedIndexValidationHelper {

    public static void validateColumnCount(int expected, String indexTypeName, int colCount) throws ExprValidationException {
        if (expected != colCount) {
            throw new ExprValidationException("Index of type '" + indexTypeName + "' requires " + expected + " expressions as index columns but received " + colCount);
        }
    }

    public static void validateParameterCount(int minExpected, int maxExpected, String indexTypeName, int paramCount) throws ExprValidationException {
        if (paramCount < minExpected || paramCount > maxExpected) {
            throw new ExprValidationException("Index of type '" + indexTypeName + "' requires at least " + minExpected + " parameters but received " + paramCount);
        }
    }

    public static void validateParameterCountEither(int expectedOne, int expectedTwo, String indexTypeName, int paramCount) throws ExprValidationException {
        if (paramCount != expectedOne && paramCount != expectedTwo) {
            throw new ExprValidationException("Index of type '" + indexTypeName + "' requires at either " + expectedOne + " or " + expectedTwo + " parameters but received " + paramCount);
        }
    }

    public static void validateColumnReturnTypeNumber(String indexTypeName, int colnum, ExprNode expr, String name) throws ExprValidationException {
        Class receivedType = expr.getForge().getEvaluationType();
        if (!isNumeric(receivedType)) {
            throw makeEx(indexTypeName, true, colnum, name, Number.class, receivedType);
        }
    }

    public static void validateParameterReturnType(Class expectedReturnType, String indexTypeName, int paramnum, ExprNode expr, String name) throws ExprValidationException {
        Class receivedType = JavaClassHelper.getBoxedType(expr.getForge().getEvaluationType());
        if (!JavaClassHelper.isSubclassOrImplementsInterface(receivedType, expectedReturnType)) {
            throw makeEx(indexTypeName, false, paramnum, name, expectedReturnType, receivedType);
        }
    }

    public static void validateParameterReturnTypeNumber(String indexTypeName, int paramnum, ExprNode expr, String name) throws ExprValidationException {
        Class receivedType = expr.getForge().getEvaluationType();
        if (!isNumeric(receivedType)) {
            throw makeEx(indexTypeName, false, paramnum, name, Number.class, receivedType);
        }
    }

    private static ExprValidationException makeEx(String indexTypeName, boolean isColumn, int num, String name, Class expectedType, Class receivedType) {
        return new ExprValidationException("Index of type '" + indexTypeName + "' for " +
                (isColumn ? "column " : "parameter ") +
                +num + " that is providing " + name + "-values expecting type " +
                JavaClassHelper.getClassNameFullyQualPretty(expectedType) +
                " but received type " + JavaClassHelper.getClassNameFullyQualPretty(receivedType));
    }
}
