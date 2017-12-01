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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.SimpleNumberCoercer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprArrayNodeForge implements ExprForge, ExprEnumerationForge {
    private final ExprArrayNode parent;
    private final Class arrayReturnType;
    private final boolean mustCoerce;
    private final SimpleNumberCoercer coercer;
    private final Object constantResult;

    public ExprArrayNodeForge(ExprArrayNode parent, Class arrayReturnType, Object[] constantResult) {
        this.parent = parent;
        this.arrayReturnType = arrayReturnType;
        this.constantResult = constantResult;
        this.mustCoerce = false;
        this.coercer = null;
    }

    public ExprArrayNodeForge(ExprArrayNode parent, Class arrayReturnType, boolean mustCoerce, SimpleNumberCoercer coercer, Object constantResult) {
        this.parent = parent;
        this.arrayReturnType = arrayReturnType;
        this.mustCoerce = mustCoerce;
        this.coercer = coercer;
        this.constantResult = constantResult;
    }

    public ExprEvaluator getExprEvaluator() {
        if (constantResult != null) {
            return new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qExprArray(parent);
                        InstrumentationHelper.get().aExprArray(constantResult);
                    }
                    return constantResult;
                }

            };
        }
        return new ExprArrayNodeForgeEval(this, ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes()));
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (constantResult != null) {
            CodegenMember array = codegenClassScope.makeAddMember(getEvaluationType(), constantResult);
            return CodegenExpressionBuilder.member(array.getMemberId());
        }
        return ExprArrayNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return constantResult != null ? ExprForgeComplexityEnum.NONE : ExprForgeComplexityEnum.INTER;
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprArrayNodeForgeEval.codegenEvaluateGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getEvaluationType() {
        return Array.newInstance(arrayReturnType, 0).getClass();
    }

    public ExprArrayNode getForgeRenderable() {
        return parent;
    }

    public Class getArrayReturnType() {
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
            return new ExprArrayNodeForgeEval(this, ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes()));
        }
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return parent.getComponentTypeCollection();
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
