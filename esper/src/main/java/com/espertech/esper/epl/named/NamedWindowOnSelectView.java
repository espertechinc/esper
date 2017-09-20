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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * View for the on-select statement that handles selecting events from a named window.
 */
public class NamedWindowOnSelectView extends NamedWindowOnExprBaseView {
    private final NamedWindowOnSelectViewFactory parent;
    private final ResultSetProcessor resultSetProcessor;
    private EventBean[] lastResult;
    private Set<MultiKey<EventBean>> oldEvents = new HashSet<MultiKey<EventBean>>();
    private final boolean audit;
    private final boolean isDelete;
    private final TableStateInstance tableStateInstanceInsertInto;

    public NamedWindowOnSelectView(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, ExprEvaluatorContext exprEvaluatorContext, NamedWindowOnSelectViewFactory parent, ResultSetProcessor resultSetProcessor, boolean audit, boolean isDelete, TableStateInstance tableStateInstanceInsertInto) {
        super(lookupStrategy, rootView, exprEvaluatorContext);
        this.parent = parent;
        this.resultSetProcessor = resultSetProcessor;
        this.audit = audit;
        this.isDelete = isDelete;
        this.tableStateInstanceInsertInto = tableStateInstanceInsertInto;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraOnAction(OnTriggerType.ON_SELECT, triggerEvents, matchingEvents);
        }

        EventBean[] newData;

        // clear state from prior results
        resultSetProcessor.clear();

        // build join result
        // use linked hash set to retain order of join results for last/first/window to work most intuitively
        Set<MultiKey<EventBean>> newEvents = buildJoinResult(triggerEvents, matchingEvents);

        // process matches
        UniformPair<EventBean[]> pair = resultSetProcessor.processJoinResult(newEvents, oldEvents, false);
        newData = pair != null ? pair.getFirst() : null;

        if (parent.isDistinct()) {
            newData = EventBeanUtility.getDistinctByProp(newData, parent.getEventBeanReader());
        }

        if (tableStateInstanceInsertInto != null) {
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    if (audit) {
                        AuditPath.auditInsertInto(getExprEvaluatorContext().getEngineURI(), getExprEvaluatorContext().getStatementName(), aNewData);
                    }
                    tableStateInstanceInsertInto.addEventUnadorned(aNewData);
                }
            }
        } else if (parent.getInternalEventRouter() != null) {
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    if (audit) {
                        AuditPath.auditInsertInto(getExprEvaluatorContext().getEngineURI(), getExprEvaluatorContext().getStatementName(), aNewData);
                    }
                    parent.getInternalEventRouter().route(aNewData, parent.getStatementHandle(), parent.getInternalEventRouteDest(), getExprEvaluatorContext(), parent.isAddToFront());
                }
            }
        }

        // The on-select listeners receive the events selected
        if ((newData != null) && (newData.length > 0)) {
            // And post only if we have listeners/subscribers that need the data
            if (parent.getStatementResultService().isMakeNatural() || parent.getStatementResultService().isMakeSynthetic()) {
                updateChildren(newData, null);
            }
        }
        lastResult = newData;

        // clear state from prior results
        resultSetProcessor.clear();

        // Events to delete are indicated via old data
        if (isDelete) {
            this.rootView.update(null, matchingEvents);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraOnAction();
        }
    }

    public static Set<MultiKey<EventBean>> buildJoinResult(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        LinkedHashSet events = new LinkedHashSet<MultiKey<EventBean>>();
        for (int i = 0; i < triggerEvents.length; i++) {
            EventBean triggerEvent = triggerEvents[0];
            if (matchingEvents != null) {
                for (int j = 0; j < matchingEvents.length; j++) {
                    EventBean[] eventsPerStream = new EventBean[2];
                    eventsPerStream[0] = matchingEvents[j];
                    eventsPerStream[1] = triggerEvent;
                    events.add(new MultiKey<EventBean>(eventsPerStream));
                }
            }
        }
        return events;
    }

    public EventType getEventType() {
        if (resultSetProcessor != null) {
            return resultSetProcessor.getResultEventType();
        } else {
            return rootView.getEventType();
        }
    }

    public Iterator<EventBean> iterator() {
        return new ArrayEventIterator(lastResult);
    }
}
