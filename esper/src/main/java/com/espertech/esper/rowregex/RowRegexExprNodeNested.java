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
 * Nested () regular expression in a regex expression tree.
 */
public class RowRegexExprNodeNested extends RowRegexExprNode {
    private final RegexNFATypeEnum type;
    private final RowRegexExprRepeatDesc optionalRepeat;
    private static final long serialVersionUID = -2079284511194587570L;

    public RowRegexExprNodeNested(RegexNFATypeEnum type, RowRegexExprRepeatDesc optionalRepeat) {
        this.type = type;
        this.optionalRepeat = optionalRepeat;
    }

    /**
     * Returns multiplicity and greedy.
     *
     * @return type
     */
    public RegexNFATypeEnum getType() {
        return type;
    }

    public RowRegexExprRepeatDesc getOptionalRepeat() {
        return optionalRepeat;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes().get(0).toEPL(writer, getPrecedence());
        writer.append(type.getOptionalPostfix());
    }

    public RowRegexExprNodePrecedenceEnum getPrecedence() {
        return RowRegexExprNodePrecedenceEnum.GROUPING;
    }
}
