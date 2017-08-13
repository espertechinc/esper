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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.core.service.ExpressionResultCacheEntryBeanAndCollBean;
import com.espertech.esper.core.service.ExpressionResultCacheForPropUnwrap;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotEventCollectionForge implements ExprEnumerationForge, ExprEnumerationEval, ExprEnumerationGivenEvent, ExprNodeRenderable {

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

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Collection.class, PropertyDotEventCollectionForge.class).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(streamId)))
                .ifRefNullReturnNull("event")
                .methodReturn(codegenEvaluateInternal(ref("event"), params, context));
        return localMethodBuild(method).passAll(params).call();
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        if (event == null) {
            return null;
        }
        return evaluateInternal(event, context);
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) {
        return fragmentType;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return constantNull();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateEventGetEventBean(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return constantNull();
    }

    private Collection<EventBean> evaluateInternal(EventBean eventInQuestion, ExprEvaluatorContext context) {

        if (disablePropertyExpressionEventCollCache) {
            EventBean[] events = (EventBean[]) getter.getFragment(eventInQuestion);
            return events == null ? null : Arrays.asList(events);
        }

        ExpressionResultCacheForPropUnwrap cache = context.getExpressionResultCacheService().getAllocateUnwrapProp();
        ExpressionResultCacheEntryBeanAndCollBean cacheEntry = cache.getPropertyColl(propertyNameCache, eventInQuestion);
        if (cacheEntry != null) {
            return cacheEntry.getResult();
        }

        EventBean[] events = (EventBean[]) getter.getFragment(eventInQuestion);
        Collection<EventBean> coll = events == null ? null : Arrays.asList(events);
        cache.savePropertyColl(propertyNameCache, eventInQuestion, coll);
        return coll;
    }

    private CodegenExpression codegenEvaluateInternal(CodegenExpressionRef event, CodegenParamSetExprPremade params, CodegenContext context) {
        if (disablePropertyExpressionEventCollCache) {
            CodegenMethodId method = context.addMethod(Collection.class, PropertyDotEventCollectionForge.class).add(EventBean.class, "event").add(params).begin()
                    .declareVar(EventBean[].class, "events", cast(EventBean[].class, getter.eventBeanFragmentCodegen(ref("event"), context)))
                    .ifRefNullReturnNull("events")
                    .methodReturn(staticMethod(Arrays.class, "asList", ref("events")));
            return localMethodBuild(method).pass(event).passAll(params).call();
        }
        CodegenMethodId method = context.addMethod(Collection.class, PropertyDotEventCollectionForge.class).add(EventBean.class, "event").add(params).begin()
                .declareVar(ExpressionResultCacheForPropUnwrap.class, "cache", exprDotMethodChain(params.passEvalCtx()).add("getExpressionResultCacheService").add("getAllocateUnwrapProp"))
                .declareVar(ExpressionResultCacheEntryBeanAndCollBean.class, "cacheEntry", exprDotMethod(ref("cache"), "getPropertyColl", constant(propertyNameCache), ref("event")))
                .ifCondition(notEqualsNull(ref("cacheEntry")))
                .blockReturn(exprDotMethod(ref("cacheEntry"), "getResult"))
                .declareVar(EventBean[].class, "events", cast(EventBean[].class, getter.eventBeanFragmentCodegen(ref("event"), context)))
                .declareVarNoInit(Collection.class, "coll")
                .ifRefNull("events")
                .assignRef("coll", constantNull())
                .ifElse()
                .assignRef("coll", staticMethod(Arrays.class, "asList", ref("events")))
                .blockEnd()
                .expression(exprDotMethod(ref("cache"), "savePropertyColl", constant(propertyNameCache), ref("event"), ref("coll")))
                .methodReturn(ref("coll"));
        return localMethodBuild(method).pass(event).passAll(params).call();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
