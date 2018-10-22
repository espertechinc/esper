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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDDelete;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDInsertInto;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodIUDUpdate;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.lang.annotation.Annotation;
import java.util.Collection;

public abstract class FireAndForgetInstance {
    public abstract EventBean[] processInsert(FAFQueryMethodIUDInsertInto insert);

    public abstract EventBean[] processDelete(FAFQueryMethodIUDDelete delete);

    public abstract EventBean[] processUpdate(FAFQueryMethodIUDUpdate update);

    public abstract Collection<EventBean> snapshotBestEffort(QueryGraph queryGraph, Annotation[] annotations);

    public abstract AgentInstanceContext getAgentInstanceContext();

    public abstract Viewable getTailViewInstance();

    //public abstract VirtualDWView getVirtualDataWindow();
}
