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
package com.espertech.esper.view;

import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.ScheduleBucket;
import com.espertech.esper.schedule.SchedulingService;

/**
 * Context calss for specific views within a statement. Each view in a statement gets it's own context
 * containing the statement context.
 */
public class ViewFactoryContext {
    private StatementContext statementContext;
    private final int streamNum;
    private final String namespaceName;
    private final String viewName;
    private final boolean isSubquery;
    private final int subqueryNumber;
    private final boolean isGrouped;

    /**
     * Ctor.
     *
     * @param statementContext is the statement-level services
     * @param streamNum        is the stream number from zero to N
     * @param namespaceName    is the view namespace
     * @param viewName         is the view name
     * @param isSubquery       subquery indicator
     * @param subqueryNumber   for subqueries
     * @param isGrouped        for grouped view
     */
    public ViewFactoryContext(StatementContext statementContext, int streamNum, String namespaceName, String viewName, boolean isSubquery, int subqueryNumber, boolean isGrouped) {
        this.statementContext = statementContext;
        this.streamNum = streamNum;
        this.namespaceName = namespaceName;
        this.viewName = viewName;
        this.isSubquery = isSubquery;
        this.subqueryNumber = subqueryNumber;
        this.isGrouped = isGrouped;
    }

    /**
     * Returns service to use for schedule evaluation.
     *
     * @return schedule evaluation service implemetation
     */
    public final SchedulingService getSchedulingService() {
        return statementContext.getSchedulingService();
    }

    /**
     * Returns service for generating events and handling event types.
     *
     * @return event adapter service
     */
    public EventAdapterService getEventAdapterService() {
        return statementContext.getEventAdapterService();
    }

    /**
     * Returns the schedule bucket for ordering schedule callbacks within this pattern.
     *
     * @return schedule bucket
     */
    public ScheduleBucket getScheduleBucket() {
        return statementContext.getScheduleBucket();
    }

    /**
     * Returns the statement's resource locks.
     *
     * @return statement resource lock/handle
     */
    public EPStatementHandle getEpStatementHandle() {
        return statementContext.getEpStatementHandle();
    }

    /**
     * Returns extension svc.
     *
     * @return svc
     */
    public StatementExtensionSvcContext getStatementExtensionServicesContext() {
        return statementContext.getStatementExtensionServicesContext();
    }

    /**
     * Returns the statement id.
     *
     * @return statement id
     */
    public int getStatementId() {
        return statementContext.getStatementId();
    }

    /**
     * Returns the stream number.
     *
     * @return stream number
     */
    public int getStreamNum() {
        return streamNum;
    }

    /**
     * Returns the view namespace name.
     *
     * @return namespace name
     */
    public String getNamespaceName() {
        return namespaceName;
    }

    /**
     * Returns the view name.
     *
     * @return view name
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Returns the statement context.
     *
     * @return statement context
     */
    public StatementContext getStatementContext() {
        return statementContext;
    }

    public boolean isSubquery() {
        return isSubquery;
    }

    public int getSubqueryNumber() {
        return subqueryNumber;
    }

    public boolean isGrouped() {
        return isGrouped;
    }

    public String toString() {
        return statementContext.toString() +
                " streamNum=" + streamNum +
                " namespaceName=" + namespaceName +
                " viewName=" + viewName;
    }
}
