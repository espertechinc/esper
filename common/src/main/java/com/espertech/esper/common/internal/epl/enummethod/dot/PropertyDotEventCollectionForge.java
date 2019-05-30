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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheEntryBeanAndCollBean;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheForPropUnwrap;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotEventCollectionForge implements ExprEnumerationForge, ExprEnumerationEval, ExprEnumerationGivenEvent, ExprEnumerationGivenEventForge, ExprNodeRenderable {

    private final String propertyNameCache;
    private final int streamId;
    private final EventType fragmentType;
    private final EventPropertyGetterSPI getter;
    private final boolean disablePropertyExpressionEventCollCache;

    public PropertyDotEventCollectionForge(String propertyNameCache, int streamId, EventType fragmentType, EventPropertyGetterSPI getter, boolean disablePropertyExpressionEventCollCache) {
        this.propertyNameCache = propertyNameCache;
        this.streamId = streamId;
        this.fragmentType = fragmentType;
        this.getter = getter;
        this.disablePropertyExpressionEventCollCache = disablePropertyExpressionEventCollCache;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean eventInQuestion = eventsPerStream[streamId];
        if (eventInQuestion == null) {
            return null;
        }
        return evaluateInternal(eventInQuestion, context);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotEventCollectionForge.class, codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        methodNode.getBlock()
                .declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(streamId)))
                .ifRefNullReturnNull("event")
                .methodReturn(codegenEvaluateInternal(ref("event"), method -> exprSymbol.getAddExprEvalCtx(method), methodNode, codegenClassScope));
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateEventGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotEventCollectionForge.class, codegenClassScope);
        methodNode.getBlock()
                .ifNullReturnNull(symbols.getAddEvent(methodNode))
                .methodReturn(codegenEvaluateInternal(symbols.getAddEvent(methodNode), method -> symbols.getAddExprEvalCtx(method), methodNode, codegenClassScope));
        return localMethod(methodNode);
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        if (event == null) {
            return null;
        }
        return evaluateInternal(event, context);
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        return fragmentType;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateEventGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateEventGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateEventGetEventBean(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    private Collection<EventBean> evaluateInternal(EventBean eventInQuestion, ExprEvaluatorContext context) {
        EventBean[] events = (EventBean[]) getter.getFragment(eventInQuestion);
        return events == null ? null : Arrays.asList(events);
    }

    private CodegenExpression codegenEvaluateInternal(CodegenExpressionRef event, Function<CodegenMethod, CodegenExpressionRef> refExprEvalCtxFunc, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (disablePropertyExpressionEventCollCache) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotEventCollectionForge.class, codegenClassScope).addParam(EventBean.class, "event");

            methodNode.getBlock()
                    .declareVar(EventBean[].class, "events", cast(EventBean[].class, getter.eventBeanFragmentCodegen(ref("event"), methodNode, codegenClassScope)))
                    .ifRefNullReturnNull("events")
                    .methodReturn(staticMethod(Arrays.class, "asList", ref("events")));
            return localMethod(methodNode, event);
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotEventCollectionForge.class, codegenClassScope).addParam(EventBean.class, "event");
        CodegenExpressionRef refExprEvalCtx = refExprEvalCtxFunc.apply(methodNode);

        methodNode.getBlock()
                .declareVar(ExpressionResultCacheForPropUnwrap.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateUnwrapProp"))
                .declareVar(ExpressionResultCacheEntryBeanAndCollBean.class, "cacheEntry", exprDotMethod(ref("cache"), "getPropertyColl", constant(propertyNameCache), ref("event")))
                .ifCondition(notEqualsNull(ref("cacheEntry")))
                .blockReturn(exprDotMethod(ref("cacheEntry"), "getResult"))
                .declareVar(EventBean[].class, "events", cast(EventBean[].class, getter.eventBeanFragmentCodegen(ref("event"), methodNode, codegenClassScope)))
                .declareVarNoInit(Collection.class, "coll")
                .ifRefNull("events")
                .assignRef("coll", constantNull())
                .ifElse()
                .assignRef("coll", staticMethod(Arrays.class, "asList", ref("events")))
                .blockEnd()
                .expression(exprDotMethod(ref("cache"), "savePropertyColl", constant(propertyNameCache), ref("event"), ref("coll")))
                .methodReturn(ref("coll"));
        return localMethod(methodNode, event);
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
