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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewSupport;

import java.util.Collections;
import java.util.Iterator;

public class TableOnMergeInsertUnmatched extends ViewSupport implements StopCallback, TableOnView {
    private final TableStateInstance tableStateInstance;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final TableMetadata metadata;
    private final TableOnMergeViewFactory parent;

    public TableOnMergeInsertUnmatched(TableStateInstance tableStateInstance, ExprEvaluatorContext exprEvaluatorContext, TableMetadata metadata, TableOnMergeViewFactory parent) {
        this.tableStateInstance = tableStateInstance;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.metadata = metadata;
        this.parent = parent;
    }

    public void stop() {
    }

    public EventType getEventType() {
        return metadata.getPublicEventType();
    }

    public Iterator<EventBean> iterator() {
        return Collections.emptyIterator();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraOnAction(OnTriggerType.ON_MERGE, newData, CollectionUtil.EVENTBEANARRAY_EMPTY);
        }

        boolean postResultsToListeners = parent.getStatementResultService().isMakeNatural() || parent.getStatementResultService().isMakeSynthetic();
        TableOnMergeViewChangeHandler changeHandlerAdded = null;
        if (postResultsToListeners) {
            changeHandlerAdded = new TableOnMergeViewChangeHandler(parent.getTableMetadata());
        }

        parent.getOnMergeHelper().getInsertUnmatched().apply(null, newData, tableStateInstance, changeHandlerAdded, null, exprEvaluatorContext);

        // The on-delete listeners receive the events deleted, but only if there is interest
        if (postResultsToListeners) {
            EventBean[] postedNew = changeHandlerAdded.getEvents();
            if (postedNew != null) {
                updateChildren(postedNew, null);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraOnAction();
        }
    }
}
