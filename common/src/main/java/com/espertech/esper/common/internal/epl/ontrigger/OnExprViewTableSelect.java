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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

import java.util.Collections;
import java.util.Set;

public class OnExprViewTableSelect extends OnExprViewTableBase {

    private final InfraOnSelectViewFactory parent;
    private final ResultSetProcessor resultSetProcessor;
    private final boolean audit;
    private final boolean deleteAndSelect;
    private final TableInstance tableInstanceInsertInto;

    public OnExprViewTableSelect(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext,
                                 ResultSetProcessor resultSetProcessor, InfraOnSelectViewFactory parent, boolean audit, boolean deleteAndSelect, TableInstance tableInstanceInsertInto) {
        super(lookupStrategy, tableInstance, agentInstanceContext, deleteAndSelect);
        this.parent = parent;
        this.resultSetProcessor = resultSetProcessor;
        this.audit = audit;
        this.deleteAndSelect = deleteAndSelect;
        this.tableInstanceInsertInto = tableInstanceInsertInto;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_SELECT, triggerEvents, matchingEvents);

        // clear state from prior results
        resultSetProcessor.clear();

        // build join result
        // use linked hash set to retain order of join results for last/first/window to work most intuitively
        Set<MultiKeyArrayOfKeys<EventBean>> newEvents = OnExprViewNamedWindowSelect.buildJoinResult(triggerEvents, matchingEvents);

        // process matches
        UniformPair<EventBean[]> pair = resultSetProcessor.processJoinResult(newEvents, Collections.<MultiKeyArrayOfKeys<EventBean>>emptySet(), false);
        EventBean[] newData = pair != null ? pair.getFirst() : null;

        // handle distinct and insert
        newData = InfraOnSelectUtil.handleDistintAndInsert(newData, parent, agentInstanceContext, tableInstanceInsertInto, audit);

        // The on-select listeners receive the events selected
        if ((newData != null) && (newData.length > 0)) {
            // And post only if we have listeners/subscribers that need the data
            StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
            if (statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic()) {
                child.update(newData, null);
            }
        }

        // clear state from prior results
        resultSetProcessor.clear();

        // Events to delete are indicated via old data
        if (deleteAndSelect) {
            for (EventBean event : matchingEvents) {
                tableInstance.deleteEvent(event);
            }
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
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
