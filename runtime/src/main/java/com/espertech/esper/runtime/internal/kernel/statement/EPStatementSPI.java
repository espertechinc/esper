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
package com.espertech.esper.runtime.internal.kernel.statement;

import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.runtime.client.EPStatement;

public interface EPStatementSPI extends EPStatement {
    /**
     * Returns the statement id.
     *
     * @return statement id
     */
    int getStatementId();

    /**
     * Returns the statement context.
     *
     * @return statement context
     */
    StatementContext getStatementContext();

    /**
     * Sets the parent view.
     *
     * @param viewable is the statement viewable
     */
    void setParentView(Viewable viewable);

    /**
     * Returns the parent view.
     *
     * @return viewable is the statement parent viewable
     */
    public Viewable getParentView();

    void recoveryUpdateListeners(EPStatementListenerSet listenerSet);

    UpdateDispatchView getDispatchChildView();

    void setDestroyed();
}
