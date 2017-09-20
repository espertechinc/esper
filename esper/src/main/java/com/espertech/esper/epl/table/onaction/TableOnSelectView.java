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
package com.espertech.esper.epl.table.onaction;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.named.NamedWindowOnSelectView;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

import java.util.Collections;
import java.util.Set;

public class TableOnSelectView extends TableOnViewBase {

    private final TableOnSelectViewFactory parent;
    private final ResultSetProcessor resultSetProcessor;
    private final boolean audit;
    private final boolean deleteAndSelect;

    public TableOnSelectView(SubordWMatchExprLookupStrategy lookupStrategy, TableStateInstance rootView, ExprEvaluatorContext exprEvaluatorContext, TableMetadata metadata,
                             TableOnSelectViewFactory parent, ResultSetProcessor resultSetProcessor, boolean audit, boolean deleteAndSelect) {
        super(lookupStrategy, rootView, exprEvaluatorContext, metadata, deleteAndSelect);
        this.parent = parent;
        this.resultSetProcessor = resultSetProcessor;
        this.audit = audit;
        this.deleteAndSelect = deleteAndSelect;
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
        Set<MultiKey<EventBean>> newEvents = NamedWindowOnSelectView.buildJoinResult(triggerEvents, matchingEvents);

        // process matches
        UniformPair<EventBean[]> pair = resultSetProcessor.processJoinResult(newEvents, Collections.<MultiKey<EventBean>>emptySet(), false);
        newData = pair != null ? pair.getFirst() : null;

        if (parent.isDistinct()) {
            newData = EventBeanUtility.getDistinctByProp(newData, parent.getEventBeanReader());
        }

        if (parent.getInternalEventRouter() != null) {
            if (newData != null) {
                for (int i = 0; i < newData.length; i++) {
                    if (audit) {
                        AuditPath.auditInsertInto(getExprEvaluatorContext().getEngineURI(), getExprEvaluatorContext().getStatementName(), newData[i]);
                    }
                    parent.getInternalEventRouter().route(newData[i], parent.getStatementHandle(), parent.getInternalEventRouteDest(), getExprEvaluatorContext(), false);
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

        // clear state from prior results
        resultSetProcessor.clear();

        // Events to delete are indicated via old data
        if (deleteAndSelect) {
            for (EventBean event : matchingEvents) {
                tableStateInstance.deleteEvent(event);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraOnAction();
        }
    }

    @Override
    public EventType getEventType() {
        if (resultSetProcessor != null) {
            return resultSetProcessor.getResultEventType();
        } else {
            return super.getEventType();
        }
    }
}
