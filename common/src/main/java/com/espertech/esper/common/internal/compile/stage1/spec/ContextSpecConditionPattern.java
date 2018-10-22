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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorPattern;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternContext;
import com.espertech.esper.common.internal.util.CollectionUtil;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecConditionPattern implements ContextSpecCondition {

    private final EvalForgeNode patternRaw;
    private final boolean inclusive;
    private final boolean immediate;

    private PatternStreamSpecCompiled patternCompiled;
    private PatternContext patternContext;

    public ContextSpecConditionPattern(EvalForgeNode patternRaw, boolean inclusive, boolean immediate) {
        this.patternRaw = patternRaw;
        this.inclusive = inclusive;
        this.immediate = immediate;
    }

    public EvalForgeNode getPatternRaw() {
        return patternRaw;
    }

    public PatternStreamSpecCompiled getPatternCompiled() {
        return patternCompiled;
    }

    public void setPatternCompiled(PatternStreamSpecCompiled patternCompiled) {
        this.patternCompiled = patternCompiled;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public void setPatternContext(PatternContext patternContext) {
        this.patternContext = patternContext;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextConditionDescriptorPattern.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextConditionDescriptorPattern.class, "condition", newInstance(ContextConditionDescriptorPattern.class))
                .exprDotMethod(ref("condition"), "setPattern", localMethod(patternCompiled.getRoot().makeCodegen(method, symbols, classScope)))
                .exprDotMethod(ref("condition"), "setPatternContext", patternContext.make(method, symbols, classScope))
                .exprDotMethod(ref("condition"), "setTaggedEvents", constant(CollectionUtil.toArray(patternCompiled.getTaggedEventTypes().keySet())))
                .exprDotMethod(ref("condition"), "setArrayEvents", constant(CollectionUtil.toArray(patternCompiled.getArrayEventTypes().keySet())))
                .exprDotMethod(ref("condition"), "setInclusive", constant(inclusive))
                .exprDotMethod(ref("condition"), "setImmediate", constant(immediate))
                .methodReturn(ref("condition"));
        return localMethod(method);
    }
}
