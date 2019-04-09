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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Arrays;

public class ExprEqualsNodeForgeNCEvalIsArrayShort extends ExprEqualsNodeForgeNCEvalBase {
    public ExprEqualsNodeForgeNCEvalIsArrayShort(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs) {
        super(parent, lhs, rhs);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        short[] left = (short[]) lhs.evaluate(eventsPerStream, isNewData, context);
        short[] right = (short[]) rhs.evaluate(eventsPerStream, isNewData, context);

        boolean result;
        if (left == null) {
            result = right == null;
        } else {
            result = right != null && Arrays.equals(left, right);
        }
        result = result ^ parent.isNotEquals();

        return result;
    }
}
