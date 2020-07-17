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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

public class FilterSpecParamContextPropForge extends FilterSpecParamForge {
    private final String propertyName;
    private final EventPropertyGetterSPI getter;
    private final SimpleNumberCoercer numberCoercer;

    public FilterSpecParamContextPropForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator, String propertyName, EventPropertyGetterSPI getter, SimpleNumberCoercer numberCoercer) {
        super(lookupable, filterOperator);
        this.propertyName = propertyName;
        this.getter = getter;
        this.numberCoercer = numberCoercer;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        CodegenMethod method = parent.makeChild(FilterSpecParam.EPTYPE, this.getClass(), classScope);

        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.EPTYPE, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
                .declareVar(ExprFilterSpecLookupable.EPTYPE_FILTEROPERATOR, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.EPTYPE, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(FilterValueSetParam.EPTYPE, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);
        getFilterValue.getBlock()
                .declareVar(EventBean.EPTYPE, "props", exprDotMethod(REF_EXPREVALCONTEXT, "getContextProperties"))
                .ifNullReturnNull(ref("props"))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "result", getter.eventBeanGetCodegen(ref("props"), method, classScope));
        if (numberCoercer != null) {
            getFilterValue.getBlock().assignRef("result", numberCoercer.coerceCodegenMayNullBoxed(cast(EPTypePremade.NUMBER.getEPType(), ref("result")), EPTypePremade.NUMBER.getEPType(), method, classScope));
        }
        getFilterValue.getBlock().methodReturn(FilterValueSetParamImpl.codegenNew(ref("result")));

        method.getBlock().methodReturn(param);
        return localMethod(method);
    }

    public void valueExprToString(StringBuilder out, int i) {
        out.append("context property '").append(propertyName).append("'");
    }
}
