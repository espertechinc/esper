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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.event.EventBeanUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SubselectEvalStrategyRowUnfilteredUnselectedTable extends SubselectEvalStrategyRowUnfilteredUnselected {

    private static final Logger log = LoggerFactory.getLogger(SubselectEvalStrategyRowUnfilteredUnselectedTable.class);

    private final TableMetadata tableMetadata;

    public SubselectEvalStrategyRowUnfilteredUnselectedTable(TableMetadata tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    @Override
    public Object evaluate(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext,
                           ExprSubselectRowNode parent) {
        if (matchingEvents.size() > 1) {
            log.warn(parent.getMultirowMessage());
            return null;
        }
        EventBean event = EventBeanUtility.getNonemptyFirstEvent(matchingEvents);
        return tableMetadata.getEventToPublic().convertToUnd(event, eventsPerStream, newData, exprEvaluatorContext);
    }
}
