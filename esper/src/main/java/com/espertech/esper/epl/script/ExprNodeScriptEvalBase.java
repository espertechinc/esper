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
package com.espertech.esper.epl.script;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.dot.ArrayWrappingCollection;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public abstract class ExprNodeScriptEvalBase implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval {

    protected final ExprNodeScript parent;
    protected final String statementName;
    protected final String[] names;
    protected final ExprForge[] parameters;
    protected final Class returnType;
    protected final EventType eventTypeCollection;
    protected final SimpleNumberCoercer coercer;

    protected abstract CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

    public ExprNodeScriptEvalBase(ExprNodeScript parent, String statementName, String[] names, ExprForge[] parameters, Class returnType, EventType eventTypeCollection) {
        this.parent = parent;
        this.statementName = statementName;
        this.names = names;
        this.parameters = parameters;
        this.returnType = returnType;
        this.eventTypeCollection = eventTypeCollection;

        if (JavaClassHelper.isNumeric(returnType)) {
            coercer = SimpleNumberCoercerFactory.getCoercer(Number.class, JavaClassHelper.getBoxedType(returnType));
        } else {
            coercer = null;
        }
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return eventTypeCollection;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = evaluate(eventsPerStream, isNewData, context);
        return scriptResultToROCollectionEvents(result);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return staticMethod(ExprNodeScriptEvalBase.class, "scriptResultToROCollectionEvents", evaluateCodegen(Collection.class, codegenMethodScope, exprSymbol, codegenClassScope));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param result script result
     * @return events
     */
    public static Collection<EventBean> scriptResultToROCollectionEvents(Object result) {
        if (result == null) {
            return null;
        }
        if (result.getClass().isArray()) {
            return Arrays.asList((EventBean[]) result);
        }
        return (Collection) result;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (returnType.isArray()) {
            return returnType.getComponentType();
        }
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        return new ArrayWrappingCollection(result);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Collection.class, ExprNodeScriptEvalBase.class, codegenClassScope);

        methodNode.getBlock()
                .declareVar(Object.class, "result", evaluateCodegen(Collection.class, methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(newInstance(ArrayWrappingCollection.class, ref("result")));
        return localMethod(methodNode);
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }
}
