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
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFATypeEnum;

import java.io.StringWriter;

/**
 * Atom in a regex expression tree.
 */
public class RowRecogExprNodeAtom extends RowRecogExprNode {
    private final String tag;
    private final RowRecogNFATypeEnum type;
    private final RowRecogExprRepeatDesc optionalRepeat;

    /**
     * Ctor.
     *
     * @param tag            variable name
     * @param type           multiplicity and greedy indicator
     * @param optionalRepeat optional repeating information
     */
    public RowRecogExprNodeAtom(String tag, RowRecogNFATypeEnum type, RowRecogExprRepeatDesc optionalRepeat) {
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
    public RowRecogNFATypeEnum getType() {
        return type;
    }

    public RowRecogExprRepeatDesc getOptionalRepeat() {
        return optionalRepeat;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(tag);
        writer.append(type.getOptionalPostfix());
        if (optionalRepeat != null) {
            optionalRepeat.toExpressionString(writer);
        }
    }

    public RowRecogExprNodePrecedenceEnum getPrecedence() {
        return RowRecogExprNodePrecedenceEnum.UNARY;
    }

    public RowRecogExprNode checkedCopySelf(ExpressionCopier expressionCopier) {
        return new RowRecogExprNodeAtom(tag, type, optionalRepeat == null ? null : optionalRepeat.checkedCopy(expressionCopier));
    }
}
