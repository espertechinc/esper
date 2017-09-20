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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.UpdateDispatchView;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.join.base.JoinExecutionStrategy;
import com.espertech.esper.epl.join.base.JoinSetIndicator;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewSupport;
import com.espertech.esper.view.Viewable;

public abstract class OutputProcessViewBase implements View, JoinSetIndicator, OutputProcessViewTerminable, StopCallback {
    protected final ResultSetProcessor resultSetProcessor;
    protected JoinExecutionStrategy joinExecutionStrategy;
    protected UpdateDispatchView childView;
    protected Viewable parentView;

    public abstract int getNumChangesetRows();

    public abstract OutputCondition getOptionalOutputCondition();

    public abstract OutputProcessViewConditionDeltaSet getOptionalDeltaSet();

    public abstract OutputProcessViewAfterState getOptionalAfterConditionState();

    protected OutputProcessViewBase(ResultSetProcessor resultSetProcessor) {
        this.resultSetProcessor = resultSetProcessor;
    }

    public Viewable getParent() {
        return parentView;
    }

    public void setParent(Viewable parent) {
        this.parentView = parent;
    }

    public View addView(View view) {
        if (childView != null) {
            throw new IllegalStateException("Child view has already been supplied");
        }
        childView = (UpdateDispatchView) view;
        return this;
    }

    public View[] getViews() {
        if (childView == null) {
            return ViewSupport.EMPTY_VIEW_ARRAY;
        }
        return new View[]{childView};
    }

    public void removeAllViews() {
        childView = null;
    }

    public boolean removeView(View view) {
        if (view != childView) {
            throw new IllegalStateException("Cannot remove child view, view has not been supplied");
        }
        childView = null;
        return true;
    }

    public boolean hasViews() {
        return childView != null;
    }

    public EventType getEventType() {
        EventType eventType = resultSetProcessor.getResultEventType();
        if (eventType != null) {
            return eventType;
        }
        return parentView.getEventType();
    }

    /**
     * For joins, supplies the join execution strategy that provides iteration over statement results.
     *
     * @param joinExecutionStrategy executes joins including static (non-continuous) joins
     */
    public void setJoinExecutionStrategy(JoinExecutionStrategy joinExecutionStrategy) {
        this.joinExecutionStrategy = joinExecutionStrategy;
    }

    public ResultSetProcessor getResultSetProcessor() {
        return resultSetProcessor;
    }
}
