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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Base for states.
 */
public abstract class RowRecogNFAStateForgeBase implements RowRecogNFAStateForge {
    private final String nodeNumNested;
    private final String variableName;
    private final int streamNum;
    private final boolean multiple;
    private final List<RowRecogNFAStateForge> nextStates;
    private final Boolean isGreedy;
    private final boolean exprRequiresMultimatchState;
    private int nodeNumFlat;

    protected abstract Class getEvalClass();

    protected abstract void assignInline(CodegenExpression eval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public RowRecogNFAStateForgeBase(String nodeNum, String variableName, int streamNum, boolean multiple, Boolean isGreedy, boolean exprRequiresMultimatchState) {
        this.nodeNumNested = nodeNum;
        this.variableName = variableName;
        this.streamNum = streamNum;
        this.multiple = multiple;
        this.isGreedy = isGreedy;
        this.exprRequiresMultimatchState = exprRequiresMultimatchState;
        nextStates = new ArrayList<>();
    }

    public final CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(getEvalClass(), this.getClass(), classScope);
        method.getBlock()
                .declareVar(getEvalClass(), "eval", newInstance(getEvalClass()))
                .exprDotMethod(ref("eval"), "setNodeNumNested", constant(nodeNumNested))
                .exprDotMethod(ref("eval"), "setVariableName", constant(variableName))
                .exprDotMethod(ref("eval"), "setStreamNum", constant(streamNum))
                .exprDotMethod(ref("eval"), "setMultiple", constant(multiple))
                .exprDotMethod(ref("eval"), "setGreedy", constant(isGreedy))
                .exprDotMethod(ref("eval"), "setNodeNumFlat", constant(nodeNumFlat))
                .exprDotMethod(ref("eval"), "setExprRequiresMultimatchState", constant(exprRequiresMultimatchState));
        assignInline(ref("eval"), method, symbols, classScope);
        method.getBlock().methodReturn(ref("eval"));
        return localMethod(method);
    }

    public int getNodeNumFlat() {
        return nodeNumFlat;
    }

    /**
     * Assign a node number.
     *
     * @param nodeNumFlat flat number
     */
    public void setNodeNumFlat(int nodeNumFlat) {
        this.nodeNumFlat = nodeNumFlat;
    }

    public String getNodeNumNested() {
        return nodeNumNested;
    }

    public List<RowRecogNFAStateForge> getNextStates() {
        return nextStates;
    }

    /**
     * Add a next state.
     *
     * @param next state to add
     */
    public void addState(RowRecogNFAStateForge next) {
        nextStates.add(next);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public String getVariableName() {
        return variableName;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public Boolean isGreedy() {
        return isGreedy;
    }

    public boolean isExprRequiresMultimatchState() {
        return exprRequiresMultimatchState;
    }
}
