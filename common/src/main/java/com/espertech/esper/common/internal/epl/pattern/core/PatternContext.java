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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMeta;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Contains handles to implementations of services needed by evaluation nodes.
 */
public class PatternContext {
    private int streamNumber;
    private MatchedEventMapMeta matchedEventMapMeta;
    private boolean isContextDeclaration;
    private int nestingLevel;
    private boolean isStartCondition;

    public PatternContext() {
    }

    public PatternContext(int streamNumber, MatchedEventMapMeta matchedEventMapMeta, boolean isContextDeclaration, int nestingLevel, boolean isStartCondition) {
        this.streamNumber = streamNumber;
        this.matchedEventMapMeta = matchedEventMapMeta;
        this.isContextDeclaration = isContextDeclaration;
        this.nestingLevel = nestingLevel;
        this.isStartCondition = isStartCondition;
    }

    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    public void setMatchedEventMapMeta(MatchedEventMapMeta matchedEventMapMeta) {
        this.matchedEventMapMeta = matchedEventMapMeta;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public MatchedEventMapMeta getMatchedEventMapMeta() {
        return matchedEventMapMeta;
    }

    public boolean isContextDeclaration() {
        return isContextDeclaration;
    }

    public void setContextDeclaration(boolean contextDeclaration) {
        isContextDeclaration = contextDeclaration;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public void setNestingLevel(int nestingLevel) {
        this.nestingLevel = nestingLevel;
    }

    public boolean isStartCondition() {
        return isStartCondition;
    }

    public void setStartCondition(boolean startCondition) {
        isStartCondition = startCondition;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PatternContext.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(PatternContext.class, "ctx", newInstance(PatternContext.class))
                .exprDotMethod(ref("ctx"), "setMatchedEventMapMeta", localMethod(matchedEventMapMeta.makeCodegen(classScope, method, symbols)));
        if (streamNumber != 0) {
            method.getBlock().exprDotMethod(ref("ctx"), "setStreamNumber", constant(streamNumber));
        }
        if (isContextDeclaration) {
            method.getBlock()
                    .exprDotMethod(ref("ctx"), "setContextDeclaration", constant(isContextDeclaration))
                    .exprDotMethod(ref("ctx"), "setNestingLevel", constant(nestingLevel))
                    .exprDotMethod(ref("ctx"), "setStartCondition", constant(isStartCondition));
        }
        method.getBlock().methodReturn(ref("ctx"));
        return localMethod(method);
    }
}
