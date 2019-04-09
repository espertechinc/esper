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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.Drop;
import com.espertech.esper.common.client.annotation.Priority;
import com.espertech.esper.common.internal.context.aifactory.update.InternalEventRouterDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethodForge;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanWriter;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.common.internal.util.TypeWidener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routing implementation that allows to pre-process events.
 */
public class InternalEventRouterImpl implements InternalEventRouter {
    private static final Logger log = LoggerFactory.getLogger(InternalEventRouterImpl.class);

    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final ConcurrentHashMap<EventType, NullableObject<InternalEventRouterPreprocessor>> preprocessors;
    private final Map<InternalEventRouterDesc, IRDescEntry> descriptors;
    private boolean hasPreprocessing = false;
    private InsertIntoListener insertIntoListener;

    public InternalEventRouterImpl(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.preprocessors = new ConcurrentHashMap<>();
        this.descriptors = new LinkedHashMap<>();
    }

    /**
     * Return true to indicate that there is pre-processing to take place.
     *
     * @return preprocessing indicator
     */
    public boolean isHasPreprocessing() {
        return hasPreprocessing;
    }

    /**
     * Pre-process the event.
     *
     * @param theEvent                            to preprocess
     * @param runtimeFilterAndDispatchTimeContext expression evaluation context
     * @param instrumentation                     instrumentation
     * @return preprocessed event
     */
    public EventBean preprocess(EventBean theEvent, ExprEvaluatorContext runtimeFilterAndDispatchTimeContext, InstrumentationCommon instrumentation) {
        return getPreprocessedEvent(theEvent, runtimeFilterAndDispatchTimeContext, instrumentation);
    }

    public void setInsertIntoListener(InsertIntoListener insertIntoListener) {
        this.insertIntoListener = insertIntoListener;
    }

    public void route(EventBean theEvent, AgentInstanceContext agentInstanceContext, boolean addToFront) {
        route(theEvent, agentInstanceContext.getStatementContext().getEpStatementHandle(), agentInstanceContext.getInternalEventRouteDest(), agentInstanceContext, addToFront);
    }

    public void route(EventBean theEvent, EPStatementHandle statementHandle, InternalEventRouteDest routeDest, ExprEvaluatorContext exprEvaluatorContext, boolean addToFront) {
        if (!hasPreprocessing) {
            if (insertIntoListener != null) {
                boolean route = insertIntoListener.inserted(theEvent, statementHandle);
                if (route) {
                    routeDest.route(theEvent, statementHandle, addToFront);
                }
            } else {
                routeDest.route(theEvent, statementHandle, addToFront);
            }
            return;
        }

        EventBean preprocessed = getPreprocessedEvent(theEvent, exprEvaluatorContext, exprEvaluatorContext.getInstrumentationProvider());
        if (preprocessed != null) {
            if (insertIntoListener != null) {
                boolean route = insertIntoListener.inserted(theEvent, statementHandle);
                if (route) {
                    routeDest.route(preprocessed, statementHandle, addToFront);
                }
            } else {
                routeDest.route(preprocessed, statementHandle, addToFront);
            }
        }
    }

    public synchronized void addPreprocessing(InternalEventRouterDesc internalEventRouterDesc, InternalRoutePreprocessView outputView, StatementContext statementContext, boolean hasSubselect) {
        descriptors.put(internalEventRouterDesc, new IRDescEntry(internalEventRouterDesc, outputView, statementContext, hasSubselect, internalEventRouterDesc.getOptionalWhereClauseEval()));

        // remove all preprocessors for this type as well as any known child types, forcing re-init on next use
        removePreprocessors(internalEventRouterDesc.getEventType());

        hasPreprocessing = true;
    }

    public synchronized void removePreprocessing(EventType eventType, InternalEventRouterDesc desc) {
        if (log.isInfoEnabled()) {
            log.debug("Removing route preprocessing for type '" + eventType.getName());
        }

        // remove all preprocessors for this type as well as any known child types
        removePreprocessors(eventType);

        descriptors.remove(desc);
        if (descriptors.isEmpty()) {
            hasPreprocessing = false;
            preprocessors.clear();
        }
    }

    private EventBean getPreprocessedEvent(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext, InstrumentationCommon instrumentation) {
        NullableObject<InternalEventRouterPreprocessor> processor = preprocessors.get(theEvent.getEventType());
        if (processor == null) {
            synchronized (this) {
                processor = initialize(theEvent.getEventType());
                preprocessors.put(theEvent.getEventType(), processor);
            }
        }

        if (processor.getObject() == null) {
            return theEvent;
        } else {
            return processor.getObject().process(theEvent, exprEvaluatorContext, instrumentation);
        }
    }

    private void removePreprocessors(EventType eventType) {
        preprocessors.remove(eventType);

        // find each child type entry
        for (EventType type : preprocessors.keySet()) {
            if (type.getDeepSuperTypes() != null) {
                for (Iterator<EventType> it = type.getDeepSuperTypes(); it.hasNext(); ) {
                    if (it.next() == eventType) {
                        preprocessors.remove(type);
                    }
                }
            }
        }
    }

    private NullableObject<InternalEventRouterPreprocessor> initialize(EventType eventType) {
        EventTypeSPI eventTypeSPI = (EventTypeSPI) eventType;
        List<InternalEventRouterEntry> desc = new ArrayList<>();

        // determine which ones to process for this types, and what priority and drop
        Set<String> eventPropertiesWritten = new HashSet<>();
        for (Map.Entry<InternalEventRouterDesc, IRDescEntry> entry : descriptors.entrySet()) {
            boolean applicable = entry.getValue().getEventType() == eventType;
            if (!applicable) {
                if (eventType.getDeepSuperTypes() != null) {
                    for (Iterator<EventType> it = eventType.getDeepSuperTypes(); it.hasNext(); ) {
                        if (it.next() == entry.getValue().getEventType()) {
                            applicable = true;
                            break;
                        }
                    }
                }
            }

            if (!applicable) {
                continue;
            }

            int priority = 0;
            boolean isDrop = false;
            Annotation[] annotations = entry.getValue().getAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                if (annotations[i] instanceof Priority) {
                    priority = ((Priority) annotations[i]).value();
                }
                if (annotations[i] instanceof Drop) {
                    isDrop = true;
                }
            }

            eventPropertiesWritten.addAll(Arrays.asList(entry.getKey().getProperties()));
            EventBeanWriter writer = eventTypeSPI.getWriter(entry.getKey().getProperties());
            desc.add(new InternalEventRouterEntry(priority, isDrop, entry.getValue().getOptionalWhereClauseEvaluator(), entry.getKey().getAssignments(), writer, entry.getValue().getWideners(), entry.getValue().getOutputView(), entry.getValue().getStatementContext(), entry.getValue().hasSubselect));
        }

        EventBeanCopyMethodForge copyMethodForge = eventTypeSPI.getCopyMethodForge(eventPropertiesWritten.toArray(new String[eventPropertiesWritten.size()]));
        if (copyMethodForge == null) {
            return new NullableObject<>(null);
        }
        return new NullableObject<>(new InternalEventRouterPreprocessor(copyMethodForge.getCopyMethod(eventBeanTypedEventFactory), desc));
    }

    private static class IRDescEntry {
        private final InternalEventRouterDesc internalEventRouterDesc;
        private final InternalRoutePreprocessView outputView;
        private final StatementContext statementContext;
        private final boolean hasSubselect;
        private final ExprEvaluator optionalWhereClauseEvaluator;

        private IRDescEntry(InternalEventRouterDesc internalEventRouterDesc, InternalRoutePreprocessView outputView, StatementContext statementContext, boolean hasSubselect, ExprEvaluator optionalWhereClauseEvaluator) {
            this.internalEventRouterDesc = internalEventRouterDesc;
            this.outputView = outputView;
            this.statementContext = statementContext;
            this.hasSubselect = hasSubselect;
            this.optionalWhereClauseEvaluator = optionalWhereClauseEvaluator;
        }

        public ExprEvaluator getOptionalWhereClauseEvaluator() {
            return optionalWhereClauseEvaluator;
        }

        public EventType getEventType() {
            return internalEventRouterDesc.getEventType();
        }

        public Annotation[] getAnnotations() {
            return internalEventRouterDesc.getAnnotations();
        }

        public TypeWidener[] getWideners() {
            return internalEventRouterDesc.getWideners();
        }

        public InternalRoutePreprocessView getOutputView() {
            return outputView;
        }

        public StatementContext getStatementContext() {
            return statementContext;
        }

        public boolean isHasSubselect() {
            return hasSubselect;
        }
    }
}
