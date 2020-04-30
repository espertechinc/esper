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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class FilterSpecParamValueNullForge extends FilterSpecParamForge {

    public FilterSpecParamValueNullForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator) {
        super(lookupable, filterOperator);
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        CodegenMethod method = parent.makeChild(FilterSpecParam.class, this.getClass(), classScope);

        method.getBlock()
            .declareVar(ExprFilterSpecLookupable.class, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
            .declareVar(FilterOperator.class, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.class, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(FilterValueSetParam.class, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);
        getFilterValue.getBlock()
            .methodReturn(FilterValueSetParamImpl.codegenNew(constantNull()));

        method.getBlock().methodReturn(param);
        return method;
    }

    public void valueExprToString(StringBuilder out, int i) {
        out.append("(no value expression)");
    }
}
