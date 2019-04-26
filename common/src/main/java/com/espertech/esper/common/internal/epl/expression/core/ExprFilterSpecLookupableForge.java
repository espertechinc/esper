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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprFilterSpecLookupableForge {
    protected final String expression;
    protected final EventPropertyValueGetterForge optionalEventPropForge;
    protected final Class returnType;
    protected final boolean isNonPropertyGetter;
    protected final DataInputOutputSerdeForge valueSerde;

    public ExprFilterSpecLookupableForge(String expression, EventPropertyValueGetterForge optionalEventPropForge, Class returnType, boolean isNonPropertyGetter, DataInputOutputSerdeForge valueSerde) {
        this.expression = expression;
        this.optionalEventPropForge = optionalEventPropForge;
        this.returnType = JavaClassHelper.getBoxedType(returnType); // For type consistency for recovery and serde define as boxed type
        this.isNonPropertyGetter = isNonPropertyGetter;
        this.valueSerde = valueSerde;
    }

    public Class getReturnType() {
        return returnType;
    }

    public String getExpression() {
        return expression;
    }

    public CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExprFilterSpecLookupable.class, ExprFilterSpecLookupableForge.class, classScope);
        CodegenExpression getterExpr;
        if (optionalEventPropForge != null) {
            CodegenExpressionNewAnonymousClass anonymous = newAnonymousClass(method.getBlock(), EventPropertyValueGetter.class);
            CodegenMethod get = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(CodegenNamedParam.from(EventBean.class, "bean"));
            anonymous.addMethod("get", get);
            get.getBlock().methodReturn(optionalEventPropForge.eventBeanGetCodegen(ref("bean"), method, classScope));
            getterExpr = anonymous;
        } else {
            getterExpr = constantNull();
        }
        method.getBlock().declareVar(EventPropertyValueGetter.class, "getter", getterExpr);

        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.class, "lookupable", newInstance(ExprFilterSpecLookupable.class,
                        constant(expression), ref("getter"), enumValue(returnType, "class"), constant(isNonPropertyGetter), valueSerde.codegen(method, classScope, null)))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSHAREDLOOKUPABLEREGISTERY).add("registerLookupable", symbols.getAddEventType(method), ref("lookupable")))
                .methodReturn(ref("lookupable"));
        return method;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprFilterSpecLookupableForge that = (ExprFilterSpecLookupableForge) o;

        if (!expression.equals(that.expression)) return false;

        return true;
    }

    public int hashCode() {
        return expression.hashCode();
    }
}

