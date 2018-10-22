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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.controller.category.ContextControllerDetailCategoryItem;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType.REF_EVENTTYPE;
import static com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType.REF_STMTINITSVC;

public class ContextSpecCategoryItem {

    private final ExprNode expression;
    private final String name;
    private FilterSpecParamForge[][] compiledFilterParam;

    public ContextSpecCategoryItem(ExprNode expression, String name) {
        this.expression = expression;
        this.name = name;
    }

    public ExprNode getExpression() {
        return expression;
    }

    public String getName() {
        return name;
    }

    public FilterSpecParamForge[][] getCompiledFilterParam() {
        return compiledFilterParam;
    }

    public void setCompiledFilterParam(FilterSpecParamForge[][] compiledFilterParam) {
        this.compiledFilterParam = compiledFilterParam;
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(ContextControllerDetailCategoryItem.class, this.getClass(), classScope).addParam(EventType.class, REF_EVENTTYPE.getRef()).addParam(EPStatementInitServices.class, REF_STMTINITSVC.getRef());

        CodegenMethod makeFilter = FilterSpecParamForge.makeParamArrayArrayCodegen(compiledFilterParam, classScope, method);
        method.getBlock()
                .declareVar(FilterSpecParam[][].class, "params", localMethod(makeFilter, REF_EVENTTYPE, REF_STMTINITSVC))
                .declareVar(ContextControllerDetailCategoryItem.class, "item", newInstance(ContextControllerDetailCategoryItem.class))
                .exprDotMethod(ref("item"), "setCompiledFilterParam", ref("params"))
                .exprDotMethod(ref("item"), "setName", constant(name))
                .methodReturn(ref("item"));
        return method;
    }
}
