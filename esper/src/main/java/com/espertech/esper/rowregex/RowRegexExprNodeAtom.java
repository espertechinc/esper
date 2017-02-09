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
 * Atom in a regex expression tree.
 */
public class RowRegexExprNodeAtom extends RowRegexExprNode {
    private final String tag;
    private final RegexNFATypeEnum type;
    private final RowRegexExprRepeatDesc optionalRepeat;
    private static final long serialVersionUID = -4844175686289523214L;

    /**
     * Ctor.
     *
     * @param tag            variable name
     * @param type           multiplicity and greedy indicator
     * @param optionalRepeat optional repeating information
     */
    public RowRegexExprNodeAtom(String tag, RegexNFATypeEnum type, RowRegexExprRepeatDesc optionalRepeat) {
        this.tag = tag;
        this.type = type;
        this.optionalRepeat = optionalRepeat;
    }

    /**
     * Returns the variable name.
     *
     * @return variable
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns multiplicity and greedy indicator.
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
        writer.append(tag);
        writer.append(type.getOptionalPostfix());
        if (optionalRepeat != null) {
            optionalRepeat.toExpressionString(writer);
        }
    }

    public RowRegexExprNodePrecedenceEnum getPrecedence() {
        return RowRegexExprNodePrecedenceEnum.UNARY;
    }
}
