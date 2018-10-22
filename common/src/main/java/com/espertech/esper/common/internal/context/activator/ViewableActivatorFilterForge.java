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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ViewableActivatorFilterForge implements ViewableActivatorForge {

    private final FilterSpecCompiled filterSpecCompiled;
    private final boolean canIterate;
    private final Integer streamNumFromClause;
    private final boolean isSubSelect;
    private final int subselectNumber;

    public ViewableActivatorFilterForge(FilterSpecCompiled filterSpecCompiled, boolean canIterate, Integer streamNumFromClause, boolean isSubSelect, int subselectNumber) {
        this.filterSpecCompiled = filterSpecCompiled;
        this.canIterate = canIterate;
        this.streamNumFromClause = streamNumFromClause;
        this.isSubSelect = isSubSelect;
        this.subselectNumber = subselectNumber;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ViewableActivatorFilter.class, this.getClass(), classScope);

        CodegenMethod makeFilter = filterSpecCompiled.makeCodegen(method, symbols, classScope);
        method.getBlock().declareVar(FilterSpecActivatable.class, "filterSpecCompiled", localMethod(makeFilter))
                .declareVar(ViewableActivatorFilter.class, "activator", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETVIEWABLEACTIVATORFACTORY).add("createFilter"))
                .exprDotMethod(ref("activator"), "setFilterSpec", ref("filterSpecCompiled"))
                .exprDotMethod(ref("activator"), "setCanIterate", constant(canIterate))
                .exprDotMethod(ref("activator"), "setStreamNumFromClause", constant(streamNumFromClause))
                .exprDotMethod(ref("activator"), "setSubSelect", constant(isSubSelect))
                .exprDotMethod(ref("activator"), "setSubselectNumber", constant(subselectNumber))
                .methodReturn(ref("activator"));
        return localMethod(method);
    }
}
