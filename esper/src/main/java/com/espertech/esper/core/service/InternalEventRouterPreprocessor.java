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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Interface for a service that routes events within the engine for further processing.
 */
public class InternalEventRouterPreprocessor {
    private static final Logger log = LoggerFactory.getLogger(InternalEventRouterPreprocessor.class);
    private static final Comparator<InternalEventRouterEntry> COMPARATOR = new Comparator<InternalEventRouterEntry>() {
        public int compare(InternalEventRouterEntry o1, InternalEventRouterEntry o2) {
            if (o1.getPriority() > o2.getPriority()) {
                return 1;
            } else if (o1.getPriority() < o2.getPriority()) {
                return -1;
            } else if (o1.isDrop()) {
                return -1;
            } else if (o2.isDrop()) {
                return -1;
            }
            return 0;
        }
    };

    private final EventBeanCopyMethod copyMethod;
    private final InternalEventRouterEntry[] entries;
    private final boolean empty;

    /**
     * Ctor.
     *
     * @param copyMethod for copying the events to preprocess
     * @param entries    descriptors for pre-processing to apply
     */
    public InternalEventRouterPreprocessor(EventBeanCopyMethod copyMethod, List<InternalEventRouterEntry> entries) {
        this.copyMethod = copyMethod;
        Collections.sort(entries, COMPARATOR);
        this.entries = entries.toArray(new InternalEventRouterEntry[entries.size()]);
        empty = this.entries.length == 0;
    }

    /**
     * Pre-proces the event.
     *
     * @param theEvent             to pre-process
     * @param exprEvaluatorContext expression evaluation context
     * @return processed event
     */
    public EventBean process(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        if (empty) {
            return theEvent;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qUpdateIStream(entries);
        }

        EventBean oldEvent = theEvent;
        boolean haveCloned = false;
        EventBean[] eventsPerStream = new EventBean[1];
        eventsPerStream[0] = theEvent;
        InternalEventRouterEntry lastEntry = null;

        for (int i = 0; i < entries.length; i++) {
            InternalEventRouterEntry entry = entries[i];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qUpdateIStreamApply(i, entry);
            }

            ExprEvaluator whereClause = entry.getOptionalWhereClause();
            if (whereClause != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qUpdateIStreamApplyWhere();
                }
                Boolean result = (Boolean) whereClause.evaluate(eventsPerStream, true, exprEvaluatorContext);
                if ((result == null) || (!result)) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aUpdateIStreamApplyWhere(result);
                    }
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aUpdateIStreamApply(null, false);
                    }
                    continue;
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aUpdateIStreamApplyWhere(true);
                }
            }

            if (entry.isDrop()) {
                return null;
            }

            // before applying the changes, indicate to last-entries output view
            if (lastEntry != null) {
                InternalRoutePreprocessView view = lastEntry.getOutputView();
                if (view.isIndicate()) {
                    EventBean copied = copyMethod.copy(theEvent);
                    view.indicate(copied, oldEvent);
                    oldEvent = copied;
                } else {
                    if (entries[i].getOutputView().isIndicate()) {
                        oldEvent = copyMethod.copy(theEvent);
                    }
                }
            }

            // copy event for the first update that applies
            if (!haveCloned) {
                EventBean copiedEvent = copyMethod.copy(theEvent);
                if (copiedEvent == null) {
                    log.warn("Event of type " + theEvent.getEventType().getName() + " could not be copied");
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aUpdateIStreamApply(null, false);
                    }
                    return null;
                }
                haveCloned = true;
                eventsPerStream[0] = copiedEvent;
                theEvent = copiedEvent;
            }

            apply(theEvent, eventsPerStream, entry, exprEvaluatorContext);
            lastEntry = entry;
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aUpdateIStreamApply(theEvent, true);
            }
        }

        if (lastEntry != null) {
            InternalRoutePreprocessView view = lastEntry.getOutputView();
            if (view.isIndicate()) {
                view.indicate(theEvent, oldEvent);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aUpdateIStream(theEvent, haveCloned);
        }
        return theEvent;
    }

    private void apply(EventBean theEvent, EventBean[] eventsPerStream, InternalEventRouterEntry entry, ExprEvaluatorContext exprEvaluatorContext) {
        // evaluate
        Object[] values;
        if (entry.isHasSubselect()) {
            entry.getAgentInstanceLock().acquireWriteLock();
            try {
                values = obtainValues(eventsPerStream, entry, exprEvaluatorContext);
            } finally {
                entry.getAgentInstanceLock().releaseWriteLock();
            }
        } else {
            values = obtainValues(eventsPerStream, entry, exprEvaluatorContext);
        }

        // apply
        entry.getWriter().write(values, theEvent);
    }

    private Object[] obtainValues(EventBean[] eventsPerStream, InternalEventRouterEntry entry, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qUpdateIStreamApplyAssignments(entry);
        }
        Object[] values = new Object[entry.getAssignments().length];
        for (int i = 0; i < entry.getAssignments().length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qUpdateIStreamApplyAssignmentItem(i);
            }
            Object value = entry.getAssignments()[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            if ((value != null) && (entry.getWideners()[i] != null)) {
                value = entry.getWideners()[i].widen(value);
            }
            values[i] = value;
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aUpdateIStreamApplyAssignmentItem(value);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aUpdateIStreamApplyAssignments(values);
        }
        return values;
    }
}
