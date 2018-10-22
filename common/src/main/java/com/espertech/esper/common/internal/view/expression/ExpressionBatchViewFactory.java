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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndexGetterImpl;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewEnum;

/**
 * Factory for {@link ExpressionBatchView}.
 */
public class ExpressionBatchViewFactory extends ExpressionViewFactoryBase {
    protected boolean includeTriggeringEvent = true;

    public View makeView(final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        ObjectArrayEventBean builtinBean = new ObjectArrayEventBean(ExpressionViewOAFieldEnum.getPrototypeOA(), builtinMapType);
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        return new ExpressionBatchView(this, viewUpdatedCollection, builtinBean, agentInstanceViewFactoryContext);
    }

    public RelativeAccessByEventNIndexGetterImpl makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public boolean isIncludeTriggeringEvent() {
        return includeTriggeringEvent;
    }

    public void setIncludeTriggeringEvent(boolean includeTriggeringEvent) {
        this.includeTriggeringEvent = includeTriggeringEvent;
    }

    public String getViewName() {
        return ViewEnum.EXPRESSION_BATCH_WINDOW.getName();
    }
}
