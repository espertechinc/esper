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
package com.espertech.esper.epl.table.merge;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.table.onaction.TableOnMergeViewChangeHandler;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.table.upd.TableUpdateStrategyReceiver;

import java.util.Collections;

public class TableOnMergeActionUpd extends TableOnMergeAction implements TableUpdateStrategyReceiver {
    private TableUpdateStrategy tableUpdateStrategy;

    public TableOnMergeActionUpd(ExprEvaluator optionalFilter, TableUpdateStrategy tableUpdateStrategy) {
        super(optionalFilter);
        this.tableUpdateStrategy = tableUpdateStrategy;
    }

    public void update(TableUpdateStrategy updateStrategy) {
        this.tableUpdateStrategy = updateStrategy;
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, TableStateInstance tableStateInstance, TableOnMergeViewChangeHandler changeHandlerAdded, TableOnMergeViewChangeHandler changeHandlerRemoved, ExprEvaluatorContext exprEvaluatorContext) {
        if (changeHandlerRemoved != null) {
            changeHandlerRemoved.add(matchingEvent, eventsPerStream, false, exprEvaluatorContext);
        }
        tableUpdateStrategy.updateTable(Collections.singleton(matchingEvent), tableStateInstance, eventsPerStream, exprEvaluatorContext);
        if (changeHandlerAdded != null) {
            changeHandlerAdded.add(matchingEvent, eventsPerStream, false, exprEvaluatorContext);
        }
    }

    public String getName() {
        return "update";
    }
}
