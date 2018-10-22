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
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorFilter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecConditionFilter implements ContextSpecCondition {

    private final FilterSpecRaw filterSpecRaw;
    private final String optionalFilterAsName;

    private FilterSpecCompiled filterSpecCompiled;

    public ContextSpecConditionFilter(FilterSpecRaw filterSpecRaw, String optionalFilterAsName) {
        this.filterSpecRaw = filterSpecRaw;
        this.optionalFilterAsName = optionalFilterAsName;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public String getOptionalFilterAsName() {
        return optionalFilterAsName;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpecCompiled) {
        this.filterSpecCompiled = filterSpecCompiled;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContextConditionDescriptorFilter.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextConditionDescriptorFilter.class, "condition", newInstance(ContextConditionDescriptorFilter.class))
                .exprDotMethod(ref("condition"), "setFilterSpecActivatable", localMethod(filterSpecCompiled.makeCodegen(method, symbols, classScope)))
                .exprDotMethod(ref("condition"), "setOptionalFilterAsName", constant(optionalFilterAsName))
                .methodReturn(ref("condition"));
        return localMethod(method);
    }
}
