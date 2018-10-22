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
package com.espertech.esper.common.client.dataflow.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

/**
 * Utility for validation data flow forge parameters
 */
public class DataFlowParameterValidation {

    /**
     * Validate the provided expression.
     *
     * @param name               parameter name
     * @param expr               expression
     * @param expectedReturnType expected result type
     * @param context            forge initialization context
     * @return validated expression node
     * @throws ExprValidationException when validation failed
     */
    public static ExprNode validate(String name, ExprNode expr, Class expectedReturnType, DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (expr == null) {
            return null;
        }
        return validate(name, expr, null, expectedReturnType, context);
    }

    /**
     * Validate the provided expression.
     *
     * @param name               parameter name
     * @param eventType          event type
     * @param expr               expression
     * @param expectedReturnType expected result type
     * @param context            forge initialization context
     * @return validated expression node
     * @throws ExprValidationException when validation failed
     */
    public static ExprNode validate(String name, ExprNode expr, EventType eventType, Class expectedReturnType, DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (expr == null) {
            return null;
        }
        ExprNode validated = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.DATAFLOWFILTER, expr, eventType, false, context.getStatementRawInfo(), context.getServices());
        validateReturnType(name, validated, expectedReturnType);
        return validated;
    }

    private static void validateReturnType(String name, ExprNode validated, Class expectedReturnType) throws ExprValidationException {
        Class returnType = validated.getForge().getEvaluationType();
        if (!JavaClassHelper.isAssignmentCompatible(JavaClassHelper.getBoxedType(returnType), expectedReturnType)) {
            String message = "Failed to validate return type of parameter '" + name +
                    "', expected '" + JavaClassHelper.getClassNameFullyQualPretty(expectedReturnType) +
                    "' but received '" + JavaClassHelper.getClassNameFullyQualPretty(returnType) + "'";
            throw new ExprValidationException(message);
        }
    }
}
