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
package com.espertech.esper.common.internal.epl.rowrecog.expr;

import com.espertech.esper.common.internal.compile.stage1.specmapper.ExpressionCopier;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import java.io.StringWriter;

public class RowRecogExprRepeatDesc {
    private final ExprNode lower;
    private final ExprNode upper;
    private final ExprNode single;

    public RowRecogExprRepeatDesc(ExprNode lower, ExprNode upper, ExprNode single) {
        this.lower = lower;
        this.upper = upper;
        this.single = single;
    }

    public ExprNode getLower() {
        return lower;
    }

    public ExprNode getUpper() {
        return upper;
    }

    public ExprNode getSingle() {
        return single;
    }

    public void toExpressionString(StringWriter writer) {
        writer.write("{");
        if (single != null) {
            writer.write(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(single));
        } else {
            if (lower != null) {
                writer.write(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(lower));
            }
            writer.write(",");
            if (upper != null) {
                writer.write(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(upper));
            }
        }
        writer.write("}");
    }

    public RowRecogExprRepeatDesc checkedCopy(ExpressionCopier expressionCopier) {
        ExprNode lowerCopy = lower == null ? null : expressionCopier.copy(lower);
        ExprNode upperCopy = upper == null ? null : expressionCopier.copy(upper);
        ExprNode singleCopy = single == null ? null : expressionCopier.copy(single);
        return new RowRecogExprRepeatDesc(lowerCopy, upperCopy, singleCopy);
    }
}
