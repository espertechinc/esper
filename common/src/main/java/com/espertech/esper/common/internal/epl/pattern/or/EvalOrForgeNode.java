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
package com.espertech.esper.common.internal.epl.pattern.or;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionPrecedenceEnum;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.io.StringWriter;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents an 'or' operator in the evaluation tree representing any event expressions.
 */
public class EvalOrForgeNode extends EvalForgeNodeBase {

    public EvalOrForgeNode(boolean attachPatternText) {
        super(attachPatternText);
    }

    public final String toString() {
        return "EvalOrNode children=" + this.getChildNodes().size();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        PatternExpressionUtil.toPrecedenceFreeEPL(writer, "or", getChildNodes(), getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.OR;
    }

    protected Class typeOfFactory() {
        return EvalOrFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "or";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().declareVar(EvalFactoryNode[].class, "children", newArrayByLength(EvalFactoryNode.class, constant(getChildNodes().size())));
        for (int i = 0; i < getChildNodes().size(); i++) {
            method.getBlock().assignArrayElement(ref("children"), constant(i), localMethod(getChildNodes().get(i).makeCodegen(method, symbols, classScope)));
        }
        method.getBlock()
                .exprDotMethod(ref("node"), "setChildren", ref("children"));
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
    }
}
