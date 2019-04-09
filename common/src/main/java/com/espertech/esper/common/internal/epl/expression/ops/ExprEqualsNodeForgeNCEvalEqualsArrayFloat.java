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

public class ExprEqualsNodeForgeNCEvalEqualsArrayFloat extends ExprEqualsNodeForgeNCEvalBase {
    public ExprEqualsNodeForgeNCEvalEqualsArrayFloat(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs) {
        super(parent, lhs, rhs);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        float[] left = (float[]) lhs.evaluate(eventsPerStream, isNewData, context);
        float[] right = (float[]) rhs.evaluate(eventsPerStream, isNewData, context);

        if (left == null || right == null) { // null comparison
            return null;
        }

        return Arrays.equals(left, right) ^ parent.isNotEquals();
    }
}
