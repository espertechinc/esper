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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.util.List;

/**
 * Match-recognize NFA states provides this information.
 */
public interface RowRecogNFAStateForge {
    /**
     * Returns the nested node number.
     *
     * @return num
     */
    public String getNodeNumNested();

    /**
     * Returns the absolute node num.
     *
     * @return num
     */
    public int getNodeNumFlat();

    /**
     * Returns the variable name.
     *
     * @return name
     */
    public String getVariableName();

    /**
     * Returns stream number.
     *
     * @return stream num
     */
    public int getStreamNum();

    /**
     * Returns greedy indicator.
     *
     * @return greedy indicator
     */
    public Boolean isGreedy();

    /**
     * Returns the next states.
     *
     * @return states
     */
    public List<RowRecogNFAStateForge> getNextStates();

    /**
     * Whether or not the match-expression requires multimatch state
     *
     * @return indicator
     */
    public boolean isExprRequiresMultimatchState();

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbol, CodegenClassScope classScope);
}
