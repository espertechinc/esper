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
 * Nested () regular expression in a regex expression tree.
 */
public class RowRecogExprNodeNested extends RowRecogExprNode {
    private final RowRecogNFATypeEnum type;
    private final RowRecogExprRepeatDesc optionalRepeat;

    public RowRecogExprNodeNested(RowRecogNFATypeEnum type, RowRecogExprRepeatDesc optionalRepeat) {
        this.type = type;
        this.optionalRepeat = optionalRepeat;
    }

    /**
     * Returns multiplicity and greedy.
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
        this.getChildNodes().get(0).toEPL(writer, getPrecedence());
        writer.append(type.getOptionalPostfix());
    }

    public RowRecogExprNodePrecedenceEnum getPrecedence() {
        return RowRecogExprNodePrecedenceEnum.GROUPING;
    }

    public RowRecogExprNode checkedCopySelf(ExpressionCopier expressionCopier) {
        return new RowRecogExprNodeNested(type, optionalRepeat == null ? null : optionalRepeat.checkedCopy(expressionCopier));
    }
}
