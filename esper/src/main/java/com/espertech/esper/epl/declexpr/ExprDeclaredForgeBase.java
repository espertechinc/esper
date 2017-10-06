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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.core.service.ExpressionResultCacheEntryEventBeanArrayAndCollBean;
import com.espertech.esper.core.service.ExpressionResultCacheEntryEventBeanArrayAndObj;
import com.espertech.esper.core.service.ExpressionResultCacheForDeclaredExprLastColl;
import com.espertech.esper.core.service.ExpressionResultCacheForDeclaredExprLastValue;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.util.AuditPath.METHOD_AUDITLOG;

public abstract class ExprDeclaredForgeBase implements ExprForge, ExprTypableReturnForge, ExprTypableReturnEval, ExprEnumerationForge, ExprEnumerationEval {
    private final ExprDeclaredNodeImpl parent;
    private final ExprForge innerForge;
    private final boolean isCache;
    private final boolean audit;
    private final String engineURI;
    private final String statementName;

    private transient ExprEvaluator innerEvaluatorLazy;
    private transient ExprEnumerationEval innerEvaluatorLambdaLazy;
    private transient ExprTypableReturnEval innerEvaluatorTypableLazy;

    public abstract EventBean[] getEventsPerStreamRewritten(EventBean[] eventsPerStream);

    protected abstract CodegenExpression codegenEventsPerStreamRewritten(CodegenExpression eventsPerStream, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);

    public ExprDeclaredForgeBase(ExprDeclaredNodeImpl parent, ExprForge innerForge, boolean isCache, boolean audit, String engineURI, String statementName) {
        this.parent = parent;
        this.innerForge = innerForge;
        this.isCache = isCache;
        this.audit = audit;
        this.engineURI = engineURI;
        this.statementName = statementName;
    }

    public ExprTypableReturnEval getTypableReturnEvaluator() {
        return this;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return innerForge.getEvaluationType();
    }

    public ExprForge getInnerForge() {
        return innerForge;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        if (innerForge instanceof ExprTypableReturnForge) {
            return ((ExprTypableReturnForge) innerForge).getRowProperties();
        }
        return null;
    }

    public Boolean isMultirow() {
        if (innerForge instanceof ExprTypableReturnForge) {
            return ((ExprTypableReturnForge) innerForge).isMultirow();
        }
        return null;
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorTypable();
        return innerEvaluatorTypableLazy.evaluateTypableSingle(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateTypableSingleCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ((ExprTypableReturnForge) innerForge).evaluateTypableSingleCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorTypable();
        return innerEvaluatorTypableLazy.evaluateTypableMulti(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ((ExprTypableReturnForge) innerForge).evaluateTypableMultiCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public final Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluator();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDeclared(parent.getPrototype());
        }

        // rewrite streams
        eventsPerStream = getEventsPerStreamRewritten(eventsPerStream);

        Object result;
        if (isCache) {      // no the same cache as for iterator
            ExpressionResultCacheForDeclaredExprLastValue cache = context.getExpressionResultCacheService().getAllocateDeclaredExprLastValue();
            ExpressionResultCacheEntryEventBeanArrayAndObj entry = cache.getDeclaredExpressionLastValue(parent.getPrototype(), eventsPerStream);
            if (entry != null) {
                return entry.getResult();
            }
            result = innerEvaluatorLazy.evaluate(eventsPerStream, isNewData, context);
            cache.saveDeclaredExpressionLastValue(parent.getPrototype(), eventsPerStream, result);
        } else {
            result = innerEvaluatorLazy.evaluate(eventsPerStream, isNewData, context);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDeclared(result);
        }
        return result;
    }

    public ExprForgeComplexityEnum getComplexity() {
        return innerForge.getComplexity();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (!audit) {
            return evaluateCodegenNoAudit(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
        }
        Class evaluationType = requiredType == Object.class ? Object.class : innerForge.getEvaluationType();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(evaluationType, ExprDeclaredForgeBase.class, codegenClassScope);
        methodNode.getBlock()
                .declareVar(evaluationType, "result", evaluateCodegenNoAudit(requiredType, methodNode, exprSymbol, codegenClassScope))
                .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                .staticMethod(AuditPath.class, METHOD_AUDITLOG, constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRDEF"), op(constant(parent.getPrototype().getName() + " result "), "+", ref("result")))
                .blockEnd()
                .methodReturn(ref("result"));
        return localMethod(methodNode);
    }

    private CodegenExpression evaluateCodegenNoAudit(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class evaluationType = requiredType == Object.class ? Object.class : innerForge.getEvaluationType();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(evaluationType, ExprDeclaredForgeBase.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean[].class, "rewritten", codegenEventsPerStreamRewritten(refEPS, methodNode, codegenClassScope))
                .methodReturn(localMethod(evaluateCodegenRewritten(requiredType, methodNode, codegenClassScope), ref("rewritten"), refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);
    }

    private CodegenMethodNode evaluateCodegenRewritten(Class requiredType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember prototype = codegenClassScope.makeAddMember(ExpressionDeclItem.class, this.parent.getPrototype());
        Class evaluationType = requiredType == Object.class ? Object.class : innerForge.getEvaluationType();

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(true, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(evaluationType, ExprDeclaredForgeBase.class, scope, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression refEPS = scope.getAddEPS(methodNode);
        CodegenExpression refExprEvalCtx = scope.getAddExprEvalCtx(methodNode);

        // generate code for the inner value so we know its symbols and derived symbols
        CodegenExpression innerValue = innerForge.evaluateCodegen(requiredType, methodNode, scope, codegenClassScope);

        // produce derived symbols
        CodegenBlock block = methodNode.getBlock();
        scope.derivedSymbolsCodegen(methodNode, block, codegenClassScope);

        if (isCache) {
            CodegenExpression eval = exprDotMethod(ref("entry"), "getResult");
            if (evaluationType != Object.class) {
                eval = cast(JavaClassHelper.getBoxedType(innerForge.getEvaluationType()), eval);
            }
            block.declareVar(ExpressionResultCacheForDeclaredExprLastValue.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastValue"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndObj.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastValue", member(prototype.getMemberId()), refEPS))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(eval)
                    .declareVar(evaluationType, "result", innerValue)
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastValue", member(prototype.getMemberId()), refEPS, ref("result")));
        } else {
            block.declareVar(evaluationType, "result", innerValue);
        }
        block.methodReturn(ref("result"));
        return methodNode;
    }

    public final Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorLambda();

        // rewrite streams
        eventsPerStream = getEventsPerStreamRewritten(eventsPerStream);

        Collection<EventBean> result;
        if (isCache) {
            ExpressionResultCacheForDeclaredExprLastColl cache = context.getExpressionResultCacheService().getAllocateDeclaredExprLastColl();
            ExpressionResultCacheEntryEventBeanArrayAndCollBean entry = cache.getDeclaredExpressionLastColl(parent.getPrototype(), eventsPerStream);
            if (entry != null) {
                return entry.getResult();
            }

            result = innerEvaluatorLambdaLazy.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
            cache.saveDeclaredExpressionLastColl(parent.getPrototype(), eventsPerStream, result);
            return result;
        } else {
            result = innerEvaluatorLambdaLazy.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
        }

        return result;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Collection.class, ExprDeclaredForgeBase.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean[].class, "rewritten", codegenEventsPerStreamRewritten(refEPS, methodNode, codegenClassScope))
                .methodReturn(localMethod(evaluateGetROCollectionEventsCodegenRewritten(methodNode, codegenClassScope), ref("rewritten"), refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);
    }

    private CodegenMethodNode evaluateGetROCollectionEventsCodegenRewritten(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember prototype = codegenClassScope.makeAddMember(ExpressionDeclItem.class, parent.getPrototype());

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(true, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, ExprDeclaredForgeBase.class, scope, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression refEPS = scope.getAddEPS(methodNode);
        CodegenExpression refExprEvalCtx = scope.getAddExprEvalCtx(methodNode);

        // generate code for the inner value so we know its symbols and derived symbols
        CodegenExpression innerValue = ((ExprEnumerationForge) innerForge).evaluateGetROCollectionEventsCodegen(methodNode, scope, codegenClassScope);

        CodegenBlock block = methodNode.getBlock();
        scope.derivedSymbolsCodegen(methodNode, block, codegenClassScope);

        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastColl.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastColl"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastColl", member(prototype.getMemberId()), refEPS))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(exprDotMethod(ref("entry"), "getResult"))
                    .declareVar(Collection.class, "result", innerValue)
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastColl", member(prototype.getMemberId()), refEPS, ref("result")));
        } else {
            block.declareVar(Collection.class, "result", innerValue);
        }
        block.methodReturn(ref("result"));
        return methodNode;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorLambda();

        // rewrite streams
        eventsPerStream = getEventsPerStreamRewritten(eventsPerStream);

        Collection result;
        if (isCache) {
            ExpressionResultCacheForDeclaredExprLastColl cache = context.getExpressionResultCacheService().getAllocateDeclaredExprLastColl();
            ExpressionResultCacheEntryEventBeanArrayAndCollBean entry = cache.getDeclaredExpressionLastColl(parent.getPrototype(), eventsPerStream);
            if (entry != null) {
                return entry.getResult();
            }

            result = innerEvaluatorLambdaLazy.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
            cache.saveDeclaredExpressionLastColl(parent.getPrototype(), eventsPerStream, result);
            return result;
        } else {
            result = innerEvaluatorLambdaLazy.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
        }

        return result;
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Collection.class, ExprDeclaredForgeBase.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean[].class, "rewritten", codegenEventsPerStreamRewritten(refEPS, methodNode, codegenClassScope))
                .methodReturn(localMethod(evaluateGetROCollectionScalarCodegenRewritten(methodNode, codegenClassScope), ref("rewritten"), refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);
    }

    private CodegenMethodNode evaluateGetROCollectionScalarCodegenRewritten(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember prototype = codegenClassScope.makeAddMember(ExpressionDeclItem.class, parent.getPrototype());

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(true, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(Collection.class, ExprDeclaredForgeBase.class, scope, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression refEPS = scope.getAddEPS(methodNode);
        CodegenExpression refExprEvalCtx = scope.getAddExprEvalCtx(methodNode);

        // generate code for the inner value so we know its symbols and derived symbols
        CodegenExpression innerValue = ((ExprEnumerationForge) innerForge).evaluateGetROCollectionScalarCodegen(methodNode, scope, codegenClassScope);

        // produce derived symbols
        CodegenBlock block = methodNode.getBlock();
        scope.derivedSymbolsCodegen(methodNode, block, codegenClassScope);

        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastColl.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastColl"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastColl", member(prototype.getMemberId()), refEPS))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(exprDotMethod(ref("entry"), "getResult"))
                    .declareVar(Collection.class, "result", innerValue)
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastColl", member(prototype.getMemberId()), refEPS, ref("result")));
        } else {
            block.declareVar(Collection.class, "result", innerValue);
        }
        block.methodReturn(ref("result"));
        return methodNode;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (innerForge instanceof ExprEnumerationForge) {
            return ((ExprEnumerationForge) innerForge).getComponentTypeCollection();
        }
        return null;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (innerForge instanceof ExprEnumerationForge) {
            return ((ExprEnumerationForge) innerForge).getEventTypeCollection(eventAdapterService, statementId);
        }
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (innerForge instanceof ExprEnumerationForge) {
            return ((ExprEnumerationForge) innerForge).getEventTypeSingle(eventAdapterService, statementId);
        }
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorLambda();
        return innerEvaluatorLambdaLazy.evaluateGetEventBean(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ((ExprEnumerationForge) innerForge).evaluateGetEventBeanCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprNode getForgeRenderable() {
        return parent;
    }

    private void initInnerEvaluator() {
        if (innerEvaluatorLazy == null) {
            innerEvaluatorLazy = innerForge.getExprEvaluator();
            if (audit) {
                innerEvaluatorLazy = (ExprEvaluator) ExprEvaluatorProxy.newInstance(engineURI, statementName, parent.getPrototype().getName(), innerEvaluatorLazy);
            }
        }
    }

    private void initInnerEvaluatorLambda() {
        if (innerForge instanceof ExprEnumerationForge && innerEvaluatorLambdaLazy == null) {
            innerEvaluatorLambdaLazy = ((ExprEnumerationForge) innerForge).getExprEvaluatorEnumeration();
        }
    }

    private void initInnerEvaluatorTypable() {
        if (innerForge instanceof ExprTypableReturnForge && innerEvaluatorTypableLazy == null) {
            innerEvaluatorTypableLazy = ((ExprTypableReturnForge) innerForge).getTypableReturnEvaluator();
        }
    }
}