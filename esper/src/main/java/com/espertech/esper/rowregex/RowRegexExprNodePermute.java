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
package com.espertech.esper.rowregex;

import java.io.StringWriter;

/**
 * Permute () regular expression in a regex expression tree.
 */
public class RowRegexExprNodePermute extends RowRegexExprNode {
    private static final long serialVersionUID = 5052642981296251751L;

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        writer.write("match_recognize_permute(");
        for (RowRegexExprNode node : this.getChildNodes()) {
            writer.write(delimiter);
            node.toEPL(writer, getPrecedence());
            delimiter = ", ";
        }
        writer.write(")");
    }

    public RowRegexExprNodePrecedenceEnum getPrecedence() {
        return RowRegexExprNodePrecedenceEnum.UNARY;
    }
}
