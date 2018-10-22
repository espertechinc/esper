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
package com.espertech.esper.common.internal.epl.rowrecog.nfa;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.util.Collections;
import java.util.List;

/**
 * End state in the regex NFA states.
 */
public class RowRecogNFAStateEndForge extends RowRecogNFAStateForgeBase {
    public RowRecogNFAStateEndForge() {
        super("endstate", null, -1, false, null, false);
    }

    public List<RowRecogNFAStateForge> getNextStates() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isExprRequiresMultimatchState() {
        throw new UnsupportedOperationException();
    }

    protected Class getEvalClass() {
        return RowRecogNFAStateEndEval.class;
    }

    protected void assignInline(CodegenExpression eval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        throw new IllegalStateException("Cannot build end state, end node is implied by node-num=-1");
    }
}
