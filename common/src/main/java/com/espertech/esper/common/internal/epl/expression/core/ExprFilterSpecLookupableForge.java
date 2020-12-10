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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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

public class ExprFilterSpecLookupableForge {
    protected final String expression;
    protected final ExprEventEvaluatorForge optionalEventEvalForge;
    protected final ExprForge optionalExprForge;
    protected final EPTypeClass returnType;
    protected final boolean isNonPropertyGetter;
    protected final DataInputOutputSerdeForge valueSerde;

    public ExprFilterSpecLookupableForge(String expression, ExprEventEvaluatorForge optionalEventEvalForge, ExprForge optionalExprForge, EPTypeClass returnType, boolean isNonPropertyGetter, DataInputOutputSerdeForge valueSerde) {
        // prefixing the expression ensures the expression resolves to either the event-eval or the expr-eval
        if (optionalExprForge != null) {
            this.expression = "." + expression;
        } else {
            this.expression = expression;
        }
        this.optionalEventEvalForge = optionalEventEvalForge;
        this.optionalExprForge = optionalExprForge;
        this.returnType = JavaClassHelper.getBoxedType(returnType); // For type consistency for recovery and serde define as boxed type
        this.isNonPropertyGetter = isNonPropertyGetter;
        this.valueSerde = valueSerde;
    }

    public EPTypeClass getReturnType() {
        return returnType;
    }

    public String getExpression() {
        return expression;
    }

    public CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExprFilterSpecLookupable.EPTYPE, ExprFilterSpecLookupableForge.class, classScope);

        CodegenExpression singleEventEvalExpr = constantNull();
        if (optionalEventEvalForge != null) {
            CodegenExpressionNewAnonymousClass anonymous = newAnonymousClass(method.getBlock(), ExprEventEvaluator.EPTYPE);
            CodegenMethod eval = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), classScope).addParam(CodegenNamedParam.from(EventBean.EPTYPE, "bean", ExprEvaluatorContext.EPTYPE, "ctx"));
            anonymous.addMethod("eval", eval);
            eval.getBlock().methodReturn(optionalEventEvalForge.eventBeanWithCtxGet(ref("bean"), ref("ctx"), method, classScope));
            singleEventEvalExpr = anonymous;
        }

        CodegenExpression epsEvalExpr = constantNull();
        if (optionalExprForge != null) {
            epsEvalExpr = ExprNodeUtilityCodegen.codegenEvaluator(optionalExprForge, method, ExprFilterSpecLookupableForge.class, classScope);
        }

        CodegenExpression serdeExpr = valueSerde == null ? constantNull() : valueSerde.codegen(method, classScope, null);
        CodegenExpression returnTypeExpr = returnType == null ? constantNull() : constant(returnType);

        method.getBlock()
            .declareVar(ExprEventEvaluator.EPTYPE, "eval", singleEventEvalExpr)
            .declareVar(ExprEvaluator.EPTYPE, "expr", epsEvalExpr)
            .declareVar(ExprFilterSpecLookupable.EPTYPE, "lookupable", newInstance(ExprFilterSpecLookupable.EPTYPE,
                constant(expression), ref("eval"), ref("expr"), returnTypeExpr, constant(isNonPropertyGetter), serdeExpr))
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

    public DataInputOutputSerdeForge getValueSerde() {
        return valueSerde;
    }
}

