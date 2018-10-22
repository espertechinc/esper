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

/**
 * Any-quantifier.
 */
public class RowRecogNFAStateAnyOneForge extends RowRecogNFAStateForgeBase {
    /**
     * Ctor.
     *
     * @param nodeNum      node num
     * @param variableName variable
     * @param streamNum    stream num
     * @param multiple     indicator
     */
    public RowRecogNFAStateAnyOneForge(String nodeNum, String variableName, int streamNum, boolean multiple) {
        super(nodeNum, variableName, streamNum, multiple, null, false);
    }

    public String toString() {
        return "AnyEvent";
    }

    protected Class getEvalClass() {
        return RowRecogNFAStateAnyOneEval.class;
    }

    protected void assignInline(CodegenExpression eval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
    }
}
