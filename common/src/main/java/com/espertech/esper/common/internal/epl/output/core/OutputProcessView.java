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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.join.base.JoinExecutionStrategy;
import com.espertech.esper.common.internal.epl.join.base.JoinSetIndicator;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.Viewable;

public abstract class OutputProcessView implements View, JoinSetIndicator, AgentInstanceStopCallback, OutputProcessViewTerminable {
    protected Viewable parentView;
    protected UpdateDispatchView child;
    protected JoinExecutionStrategy joinExecutionStrategy;

    public Viewable getParent() {
        return parentView;
    }

    public void setParent(Viewable parent) {
        this.parentView = parent;
    }

    public void setChild(View view) {
        child = (UpdateDispatchView) view;
    }

    public View getChild() {
        return child;
    }

    public void setJoinExecutionStrategy(JoinExecutionStrategy execution) {
        this.joinExecutionStrategy = execution;
    }

    public abstract int getNumChangesetRows();

    public abstract OutputCondition getOptionalOutputCondition();
}
