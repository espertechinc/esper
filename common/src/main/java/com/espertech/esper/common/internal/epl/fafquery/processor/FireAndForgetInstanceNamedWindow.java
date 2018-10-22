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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDDelete;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDInsertInto;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDUpdate;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.lang.annotation.Annotation;
import java.util.Collection;

public class FireAndForgetInstanceNamedWindow extends FireAndForgetInstance {
    private final NamedWindowInstance processorInstance;

    public FireAndForgetInstanceNamedWindow(NamedWindowInstance processorInstance) {
        this.processorInstance = processorInstance;
    }

    public NamedWindowInstance getProcessorInstance() {
        return processorInstance;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return processorInstance.getTailViewInstance().getAgentInstanceContext();
    }

    public Collection<EventBean> snapshotBestEffort(QueryGraph queryGraph, Annotation[] annotations) {
        return processorInstance.getTailViewInstance().snapshot(queryGraph, annotations);
    }

    public EventBean[] processInsert(FAFQueryMethodIUDInsertInto insert) {
        AgentInstanceContext ctx = processorInstance.getTailViewInstance().getAgentInstanceContext();
        try {
            EventBean event = insert.getInsertHelper().process(new EventBean[0], true, true, ctx);
            EventBean[] inserted = new EventBean[]{event};

            StatementAgentInstanceLock ailock = ctx.getAgentInstanceLock();
            ailock.acquireWriteLock();
            try {
                processorInstance.getRootViewInstance().update(inserted, null);
            } catch (EPException ex) {
                processorInstance.getRootViewInstance().update(null, inserted);
            } finally {
                ailock.releaseWriteLock();
            }
            return inserted;
        } finally {
            ctx.getTableExprEvaluatorContext().releaseAcquiredLocks();
        }
    }

    public EventBean[] processDelete(FAFQueryMethodIUDDelete delete) {
        return processorInstance.getTailViewInstance().snapshotDelete(delete.getQueryGraph(), delete.getOptionalWhereClause(), delete.getAnnotations());
    }

    public EventBean[] processUpdate(FAFQueryMethodIUDUpdate update) {
        return processorInstance.getTailViewInstance().snapshotUpdate(update.getQueryGraph(), update.getOptionalWhereClause(), update.getUpdateHelperNamedWindow(), update.getAnnotations());
    }

    public Viewable getTailViewInstance() {
        return processorInstance.getTailViewInstance();
    }
}
