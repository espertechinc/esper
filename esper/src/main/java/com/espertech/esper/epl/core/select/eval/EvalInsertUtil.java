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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class EvalInsertUtil {

    public static ExprValidationException makeEventTypeCastException(EventType sourceType, EventType targetType) {
        return new ExprValidationException("Expression-returned event type '" + sourceType.getName() +
                "' with underlying type '" + sourceType.getUnderlyingType().getName() +
                "' cannot be converted to target event type '" + targetType.getName() +
                "' with underlying type '" + targetType.getUnderlyingType().getName() + "'");
    }

    public static ExprValidationException makeEventTypeCastException(Class sourceType, EventType targetType) {
        return new ExprValidationException("Expression-returned value of type '" + sourceType.getName() +
                "' cannot be converted to target event type '" + targetType.getName() +
                "' with underlying type '" + targetType.getUnderlyingType().getName() + "'");
    }
}
