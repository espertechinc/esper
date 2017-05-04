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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.view.Viewable;

import java.lang.annotation.Annotation;
import java.util.Collection;

public abstract class FireAndForgetInstance {
    public abstract EventBean[] processInsert(EPPreparedExecuteIUDSingleStreamExecInsert insert);

    public abstract EventBean[] processDelete(EPPreparedExecuteIUDSingleStreamExecDelete delete);

    public abstract EventBean[] processUpdate(EPPreparedExecuteIUDSingleStreamExecUpdate update);

    public abstract Collection<EventBean> snapshotBestEffort(EPPreparedExecuteMethodQuery epPreparedExecuteMethodQuery, QueryGraph queryGraph, Annotation[] annotations);

    public abstract AgentInstanceContext getAgentInstanceContext();

    public abstract Viewable getTailViewInstance();

    public abstract VirtualDWView getVirtualDataWindow();

}
