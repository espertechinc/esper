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

import java.io.StringWriter;

/**
 * Permute () regular expression in a regex expression tree.
 */
public class RowRecogExprNodePermute extends RowRecogExprNode {
    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        writer.write("match_recognize_permute(");
        for (RowRecogExprNode node : this.getChildNodes()) {
            writer.write(delimiter);
            node.toEPL(writer, getPrecedence());
            delimiter = ", ";
        }
        writer.write(")");
    }

    public RowRecogExprNodePrecedenceEnum getPrecedence() {
        return RowRecogExprNodePrecedenceEnum.UNARY;
    }

    public RowRecogExprNode checkedCopySelf(ExpressionCopier expressionCopier) {
        return new RowRecogExprNodePermute();
    }
}
