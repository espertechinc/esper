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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprFilterSpecLookupableFactoryForgePremade implements ExprFilterSpecLookupableFactoryForge {
    protected final String expression;
    protected final ExprEventEvaluatorForge optionalEventEvalForge;
    protected final Class returnType;
    protected final boolean isNonPropertyGetter;
    protected final DataInputOutputSerdeForge valueSerde;

    public ExprFilterSpecLookupableFactoryForgePremade(String expression, ExprEventEvaluatorForge optionalEventEvalForge, Class returnType, boolean isNonPropertyGetter, DataInputOutputSerdeForge valueSerde) {
        this.expression = expression;
        this.optionalEventEvalForge = optionalEventEvalForge;
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
        CodegenMethod method = parent.makeChild(ExprFilterSpecLookupable.class, ExprFilterSpecLookupableFactoryForgePremade.class, classScope);
        CodegenExpression evalExpr;
        if (optionalEventEvalForge != null) {
            CodegenExpressionNewAnonymousClass anonymous = newAnonymousClass(method.getBlock(), ExprEventEvaluator.class);
            CodegenMethod eval = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(CodegenNamedParam.from(EventBean.class, "bean", ExprEvaluatorContext.class, "ctx"));
            anonymous.addMethod("eval", eval);
            eval.getBlock().methodReturn(optionalEventEvalForge.eventBeanWithCtxGet(ref("bean"), ref("ctx"), method, classScope));
            evalExpr = anonymous;
        } else {
            evalExpr = constantNull();
        }
        method.getBlock().declareVar(ExprEventEvaluator.class, "eval", evalExpr);

        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.class, "lookupable", newInstance(ExprFilterSpecLookupable.class,
                        constant(expression), ref("eval"), enumValue(returnType, "class"), constant(isNonPropertyGetter), valueSerde.codegen(method, classScope, null)))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSHAREDLOOKUPABLEREGISTERY).add("registerLookupable", symbols.getAddEventType(method), ref("lookupable")))
                .methodReturn(ref("lookupable"));
        return method;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprFilterSpecLookupableFactoryForgePremade that = (ExprFilterSpecLookupableFactoryForgePremade) o;

        if (!expression.equals(that.expression)) return false;

        return true;
    }

    public int hashCode() {
        return expression.hashCode();
    }
}

