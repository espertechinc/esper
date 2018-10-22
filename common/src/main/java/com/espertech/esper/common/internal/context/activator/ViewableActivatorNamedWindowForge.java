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
import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ViewableActivatorNamedWindowForge implements ViewableActivatorForge {

    private final NamedWindowConsumerStreamSpec spec;
    private final NamedWindowMetaData namedWindow;
    private final ExprNode filterEvaluator;
    private final QueryGraphForge filterQueryGraph;
    private final boolean subquery;
    private final PropertyEvaluatorForge optPropertyEvaluator;

    public ViewableActivatorNamedWindowForge(NamedWindowConsumerStreamSpec spec, NamedWindowMetaData namedWindow, ExprNode filterEvaluator, QueryGraphForge filterQueryGraph, boolean subquery, PropertyEvaluatorForge optPropertyEvaluator) {
        this.spec = spec;
        this.namedWindow = namedWindow;
        this.filterEvaluator = filterEvaluator;
        this.filterQueryGraph = filterQueryGraph;
        this.subquery = subquery;
        this.optPropertyEvaluator = optPropertyEvaluator;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (spec.getNamedWindowConsumerId() == -1) {
            throw new IllegalStateException("Unassigned named window consumer id");
        }
        CodegenMethod method = parent.makeChild(ViewableActivatorNamedWindow.class, this.getClass(), classScope);

        CodegenExpression filter;
        if (filterEvaluator == null) {
            filter = constantNull();
        } else {
            filter = ExprNodeUtilityCodegen.codegenEvaluator(filterEvaluator.getForge(), method, this.getClass(), classScope);
        }

        method.getBlock()
                .declareVar(ViewableActivatorNamedWindow.class, "activator", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETVIEWABLEACTIVATORFACTORY).add("createNamedWindow"))
                .exprDotMethod(ref("activator"), "setNamedWindow", NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindow, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("activator"), "setNamedWindowConsumerId", constant(spec.getNamedWindowConsumerId()))
                .exprDotMethod(ref("activator"), "setFilterEvaluator", filter)
                .exprDotMethod(ref("activator"), "setFilterQueryGraph", filterQueryGraph == null ? constantNull() : filterQueryGraph.make(method, symbols, classScope))
                .exprDotMethod(ref("activator"), "setSubquery", constant(subquery))
                .exprDotMethod(ref("activator"), "setOptPropertyEvaluator", optPropertyEvaluator == null ? constantNull() : optPropertyEvaluator.make(method, symbols, classScope))
                .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", ref("activator")) // add ready-callback
                .methodReturn(ref("activator"));
        return localMethod(method);
    }
}
