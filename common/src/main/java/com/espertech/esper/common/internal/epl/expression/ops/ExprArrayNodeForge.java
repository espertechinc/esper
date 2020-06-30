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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprArrayNodeForge implements ExprForgeInstrumentable, ExprEnumerationForge {
    private final ExprArrayNode parent;
    private final EPTypeClass arrayReturnType;
    private final boolean mustCoerce;
    private final SimpleNumberCoercer coercer;
    private final Object constantResult;

    public ExprArrayNodeForge(ExprArrayNode parent, EPTypeClass arrayReturnType, Object constantResult) {
        this.parent = parent;
        this.arrayReturnType = arrayReturnType;
        this.constantResult = constantResult;
        this.mustCoerce = false;
        this.coercer = null;
    }

    public ExprArrayNodeForge(ExprArrayNode parent, EPTypeClass arrayReturnType, boolean mustCoerce, SimpleNumberCoercer coercer, Object constantResult) {
        this.parent = parent;
        this.arrayReturnType = arrayReturnType;
        this.mustCoerce = mustCoerce;
        this.coercer = coercer;
        this.constantResult = constantResult;
    }

    public ExprForgeConstantType getForgeConstantType() {
        if (constantResult != null) {
            return ExprForgeConstantType.COMPILETIMECONST;
        } else {
            return ExprForgeConstantType.NONCONST;
        }
    }

    public ExprEvaluator getExprEvaluator() {
        if (constantResult != null) {
            return new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    return constantResult;
                }
            };
        }
        return new ExprArrayNodeForgeEval(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(parent.getChildNodes()));
    }

    public CodegenExpression evaluateCodegenUninstrumented(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (constantResult != null) {
            return constant(constantResult);
        }
        return ExprArrayNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprArray", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprArrayNodeForgeEval.codegenEvaluateGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public EPTypeClass getEvaluationType() {
        return JavaClassHelper.getArrayType(arrayReturnType);
    }

    public ExprArrayNode getForgeRenderable() {
        return parent;
    }

    public EPTypeClass getArrayReturnType() {
        return arrayReturnType;
    }

    public boolean isMustCoerce() {
        return mustCoerce;
    }

    public SimpleNumberCoercer getCoercer() {
        return coercer;
    }

    public Object getConstantResult() {
        return constantResult;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        if (constantResult != null) {
            final ArrayList constantResultList = new ArrayList();
            for (int i = 0; i < parent.getChildNodes().length; i++) {
                constantResultList.add(Array.get(constantResult, i));
            }
            return new ExprEnumerationEval() {

                public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    return null;
                }

                public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    return constantResultList;
                }

                public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    return null;
                }
            };
        } else {
            return new ExprArrayNodeForgeEval(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(parent.getChildNodes()));
        }
    }

    public ExprArrayNode getParent() {
        return parent;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public EPTypeClass getComponentTypeCollection() throws ExprValidationException {
        return parent.getComponentTypeCollection();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
