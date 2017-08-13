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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.core.service.ExpressionResultCacheEntryEventBeanArrayAndCollBean;
import com.espertech.esper.core.service.ExpressionResultCacheEntryEventBeanArrayAndObj;
import com.espertech.esper.core.service.ExpressionResultCacheForDeclaredExprLastColl;
import com.espertech.esper.core.service.ExpressionResultCacheForDeclaredExprLastValue;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashMap;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

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

    protected abstract CodegenExpression codegenEventsPerStreamRewritten(CodegenExpression eventsPerStream, CodegenContext context);

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

    public CodegenExpression evaluateTypableSingleCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ((ExprTypableReturnForge) innerForge).evaluateTypableSingleCodegen(params, context);
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        initInnerEvaluatorTypable();
        return innerEvaluatorTypableLazy.evaluateTypableMulti(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ((ExprTypableReturnForge) innerForge).evaluateTypableMultiCodegen(params, context);
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

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        if (!audit) {
            return evaluateCodegenNoAudit(params, context);
        }
        CodegenMethodId method = context.addMethod(innerForge.getEvaluationType(), ExprDeclaredForgeBase.class).add(params).begin()
                .declareVar(innerForge.getEvaluationType(), "result", evaluateCodegenNoAudit(params, context))
                .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                .expression(staticMethod(AuditPath.class, "auditLog", constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRDEF"), op(constant(parent.getPrototype().getName() + " result "), "+", ref("result"))))
                .blockEnd()
                .methodReturn(ref("result"));
        return localMethodBuild(method).passAll(params).call();
    }

    private CodegenExpression evaluateCodegenNoAudit(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember prototype = context.makeAddMember(ExpressionDeclItem.class, parent.getPrototype());
        CodegenBlock block = context.addMethod(innerForge.getEvaluationType(), ExprDeclaredForgeBase.class).add(params).begin()
                .assignRef(params.EPS_NAME, codegenEventsPerStreamRewritten(params.passEPS(), context));
        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastValue.class, "cache", exprDotMethodChain(params.passEvalCtx()).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastValue"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndObj.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastValue", member(prototype.getMemberId()), params.passEPS()))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(cast(JavaClassHelper.getBoxedType(innerForge.getEvaluationType()), exprDotMethod(ref("entry"), "getResult")))
                    .declareVar(innerForge.getEvaluationType(), "result", innerForge.evaluateCodegen(params, context))
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastValue", member(prototype.getMemberId()), params.passEPS(), ref("result")));
        } else {
            block.declareVar(innerForge.getEvaluationType(), "result", innerForge.evaluateCodegen(params, context));
        }
        return localMethodBuild(block.methodReturn(ref("result"))).passAll(params).call();
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

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember prototype = context.makeAddMember(ExpressionDeclItem.class, parent.getPrototype());
        CodegenBlock block = context.addMethod(Collection.class, ExprDeclaredForgeBase.class).add(params).begin()
                .assignRef(params.EPS_NAME, codegenEventsPerStreamRewritten(params.passEPS(), context));
        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastColl.class, "cache", exprDotMethodChain(params.passEvalCtx()).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastColl"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastColl", member(prototype.getMemberId()), params.passEPS()))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(exprDotMethod(ref("entry"), "getResult"))
                    .declareVar(Collection.class, "result", ((ExprEnumerationForge) innerForge).evaluateGetROCollectionEventsCodegen(params, context))
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastColl", member(prototype.getMemberId()), params.passEPS(), ref("result")));
        } else {
            block.declareVar(Collection.class, "result", ((ExprEnumerationForge) innerForge).evaluateGetROCollectionEventsCodegen(params, context));
        }
        return localMethodBuild(block.methodReturn(ref("result"))).passAll(params).call();
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

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember prototype = context.makeAddMember(ExpressionDeclItem.class, parent.getPrototype());
        CodegenBlock block = context.addMethod(Collection.class, ExprDeclaredForgeBase.class).add(params).begin()
                .assignRef(params.EPS_NAME, codegenEventsPerStreamRewritten(params.passEPS(), context));
        if (isCache) {
            block.declareVar(ExpressionResultCacheForDeclaredExprLastColl.class, "cache", exprDotMethodChain(params.passEvalCtx()).add("getExpressionResultCacheService").add("getAllocateDeclaredExprLastColl"))
                    .declareVar(ExpressionResultCacheEntryEventBeanArrayAndCollBean.class, "entry", exprDotMethod(ref("cache"), "getDeclaredExpressionLastColl", member(prototype.getMemberId()), params.passEPS()))
                    .ifCondition(notEqualsNull(ref("entry")))
                    .blockReturn(exprDotMethod(ref("entry"), "getResult"))
                    .declareVar(Collection.class, "result", ((ExprEnumerationForge) innerForge).evaluateGetROCollectionScalarCodegen(params, context))
                    .expression(exprDotMethod(ref("cache"), "saveDeclaredExpressionLastColl", member(prototype.getMemberId()), params.passEPS(), ref("result")));
        } else {
            block.declareVar(Collection.class, "result", ((ExprEnumerationForge) innerForge).evaluateGetROCollectionScalarCodegen(params, context));
        }
        return localMethodBuild(block.methodReturn(ref("result"))).passAll(params).call();
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

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ((ExprEnumerationForge) innerForge).evaluateGetEventBeanCodegen(params, context);
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