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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheEntryEventBeanArrayAndCollBean;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheEntryEventBeanArrayAndObj;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheForDeclaredExprLastColl;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheForDeclaredExprLastValue;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.runtime.ExpressionDeployTimeResolver;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class ExprDeclaredForgeBase implements ExprForgeInstrumentable, ExprTypableReturnForge, ExprTypableReturnEval, ExprEnumerationForge, ExprEnumerationEval {
    private final ExprDeclaredNodeImpl parent;
    private final ExprForge innerForge;
    private final boolean isCache;
    private final boolean audit;
    private final String statementName;

    private transient ExprEvaluator innerEvaluatorLazy;
    private transient ExprEnumerationEval innerEvaluatorLambdaLazy;
    private transient ExprTypableReturnEval innerEvaluatorTypableLazy;

    public abstract EventBean[] getEventsPerStreamRewritten(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    protected abstract CodegenExpression codegenEventsPerStreamRewritten(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

    public ExprDeclaredForgeBase(ExprDeclaredNodeImpl parent, ExprForge innerForge, boolean isCache, boolean audit, String statementName) {
        this.parent = parent;
        this.innerForge = innerForge;
        this.isCache = isCache;
        this.audit = audit;
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
        eventsPerStream = getEventsPerStreamRewritten(eventsPerStream, isNewData, context);
        return innerEvaluatorLazy.evaluate(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (!audit) {
            return evaluateCodegenNoAudit(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
        }
        Class evaluationType = requiredType == Object.class ? Object.class : innerForge.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(evaluationType, ExprDeclaredForgeBase.class, codegenClassScope);
        methodNode.getBlock()
                .declareVar(evaluationType, "result", evaluateCodegenNoAudit(requiredType, methodNode, exprSymbol, codegenClassScope))
                .expression(exprDotMethodChain(exprSymbol.getAddExprEvalCtx(methodNode)).add("getAuditProvider").add("exprdef", constant(parent.getPrototype().getName()), ref("result"), exprSymbol.getAddExprEvalCtx(methodNode)))
                .methodReturn(ref("result"));
        return localMethod(methodNode);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return null;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprDeclared", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).qparams(getInstrumentationQParams(parent, codegenClassScope)).build();
    }

    protected static CodegenExpression[] getInstrumentationQParams(ExprDeclaredNodeImpl parent, CodegenClassScope codegenClassScope) {
        String expressionText = null;
        if (codegenClassScope.isInstrumented()) {
            expressionText = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(parent.getExpressionBodyCopy());
        }
        return new CodegenExpression[]{
                constant(parent.getPrototype().getName()),
                constant(expressionText),
                constant(parent.getPrototype().getParametersNames())
        };
    }

    private CodegenExpression evaluateCodegenNoAudit(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class evaluationType = requiredType == Object.class ? Object.class : innerForge.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(evaluationType, ExprDeclaredForgeBase.class, codegenClassScope);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean[].class, "rewritten", codegenEventsPerStreamRewritten(methodNode, exprSymbol, codegenClassScope))
                .methodReturn(localMethod(evaluateCodegenRewritten(requiredType, methodNode, codegenClassScope), ref("rewritten"), refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);
    }

    private CodegenMethod evaluateCodegenRewritten(Class requiredType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField nodeObject = getNodeObject(codegenClassScope);
        Class evaluationType = requiredType == Object.class ? Object.class : innerForge.getEvaluationType();

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(evaluationType, ExprDeclaredForgeBase.class, scope, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
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
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndObj.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastValue", nodeObject, refEPS))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(eval)
                    .declareVar(evaluationType, "result", innerValue)
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastValue", nodeObject, refEPS, ref("result")));
        } else {
            block.declareVar(evaluationType, "result", innerValue);
        }
        block.methodReturn(ref("result"));
        return methodNode;
    }

    public final Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorLambda();
        eventsPerStream = getEventsPerStreamRewritten(eventsPerStream, isNewData, context);
        Collection<EventBean> result = innerEvaluatorLambdaLazy.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
        return result;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, ExprDeclaredForgeBase.class, codegenClassScope);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean[].class, "rewritten", codegenEventsPerStreamRewritten(methodNode, exprSymbol, codegenClassScope))
                .methodReturn(localMethod(evaluateGetROCollectionEventsCodegenRewritten(methodNode, codegenClassScope), ref("rewritten"), refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);
    }

    private CodegenMethod evaluateGetROCollectionEventsCodegenRewritten(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField nodeObject = getNodeObject(codegenClassScope);

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(Collection.class, ExprDeclaredForgeBase.class, scope, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression refEPS = scope.getAddEPS(methodNode);
        CodegenExpression refExprEvalCtx = scope.getAddExprEvalCtx(methodNode);

        // generate code for the inner value so we know its symbols and derived symbols
        CodegenExpression innerValue = ((ExprEnumerationForge) innerForge).evaluateGetROCollectionEventsCodegen(methodNode, scope, codegenClassScope);

        CodegenBlock block = methodNode.getBlock();
        scope.derivedSymbolsCodegen(methodNode, block, codegenClassScope);

        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastColl.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastColl"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastColl", nodeObject, refEPS))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(exprDotMethod(ref("entry"), "getResult"))
                    .declareVar(Collection.class, "result", innerValue)
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastColl", nodeObject, refEPS, ref("result")));
        } else {
            block.declareVar(Collection.class, "result", innerValue);
        }
        block.methodReturn(ref("result"));
        return methodNode;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorLambda();
        eventsPerStream = getEventsPerStreamRewritten(eventsPerStream, isNewData, context);
        return innerEvaluatorLambdaLazy.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, ExprDeclaredForgeBase.class, codegenClassScope);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean[].class, "rewritten", codegenEventsPerStreamRewritten(methodNode, exprSymbol, codegenClassScope))
                .methodReturn(localMethod(evaluateGetROCollectionScalarCodegenRewritten(methodNode, codegenClassScope), ref("rewritten"), refIsNewData, refExprEvalCtx));
        return localMethod(methodNode);
    }

    private CodegenMethod evaluateGetROCollectionScalarCodegenRewritten(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField nodeObject = getNodeObject(codegenClassScope);
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(Collection.class, ExprDeclaredForgeBase.class, scope, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression refEPS = scope.getAddEPS(methodNode);
        CodegenExpression refExprEvalCtx = scope.getAddExprEvalCtx(methodNode);

        // generate code for the inner value so we know its symbols and derived symbols
        CodegenExpression innerValue = ((ExprEnumerationForge) innerForge).evaluateGetROCollectionScalarCodegen(methodNode, scope, codegenClassScope);

        // produce derived symbols
        CodegenBlock block = methodNode.getBlock();
        scope.derivedSymbolsCodegen(methodNode, block, codegenClassScope);

        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastColl.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastColl"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastColl", nodeObject, refEPS))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(exprDotMethod(ref("entry"), "getResult"))
                    .declareVar(Collection.class, "result", innerValue)
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastColl", nodeObject, refEPS, ref("result")));
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

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (innerForge instanceof ExprEnumerationForge) {
            return ((ExprEnumerationForge) innerForge).getEventTypeCollection(statementRawInfo, compileTimeServices);
        }
        return null;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (innerForge instanceof ExprEnumerationForge) {
            return ((ExprEnumerationForge) innerForge).getEventTypeSingle(statementRawInfo, compileTimeServices);
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
        }
    }

    private void initInnerEvaluatorLambda() {
        if (innerForge instanceof ExprEnumerationForge && innerEvaluatorLambdaLazy == null) {
            innerEvaluatorLambdaLazy = ((ExprEnumerationForge) innerForge).getExprEvaluatorEnumeration();
        }
    }

    private void initInnerEvaluatorTypable() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    private CodegenExpressionField getNodeObject(CodegenClassScope codegenClassScope) {
        return ExpressionDeployTimeResolver.makeRuntimeCacheKeyField(parent.getPrototypeWVisibility(), codegenClassScope, this.getClass());
    }
}