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
 * Concatenation of atoms in a regular expression tree.
 */
public class RowRegexExprNodeConcatenation extends RowRegexExprNode {
    private static final long serialVersionUID = 2450243642083341825L;

    /**
     * Ctor.
     */
    public RowRegexExprNodeConcatenation() {
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        for (RowRegexExprNode node : this.getChildNodes()) {
            writer.append(delimiter);
            node.toEPL(writer, getPrecedence());
            delimiter = " ";
        }
    }

    public RowRegexExprNodePrecedenceEnum getPrecedence() {
        return RowRegexExprNodePrecedenceEnum.CONCATENATION;
    }
}
