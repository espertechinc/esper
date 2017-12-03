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
package com.espertech.esper.pattern;

import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.ExceptionHandlingService;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.schedule.ScheduleBucket;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.TimeProvider;

/**
 * Contains handles to implementations of services needed by evaluation nodes.
 */
public class PatternContext {
    private final int streamNumber;
    private final StatementContext statementContext;
    private final MatchedEventMapMeta matchedEventMapMeta;
    private final boolean isResilient;

    public PatternContext(StatementContext statementContext,
                          int streamNumber,
                          MatchedEventMapMeta matchedEventMapMeta,
                          boolean isResilient) {
        this.streamNumber = streamNumber;
        this.statementContext = statementContext;
        this.matchedEventMapMeta = matchedEventMapMeta;
        this.isResilient = isResilient;
    }

    /**
     * Returns service to use for filter evaluation.
     *
     * @return filter evaluation service implemetation
     */
    public final FilterService getFilterService() {
        return statementContext.getFilterService();
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
     * Returns the schedule bucket for ordering schedule callbacks within this pattern.
     *
     * @return schedule bucket
     */
    public ScheduleBucket getScheduleBucket() {
        return statementContext.getScheduleBucket();
    }

    /**
     * Returns teh service providing event adaptering or wrapping.
     *
     * @return event adapter service
     */
    public EventAdapterService getEventAdapterService() {
        return statementContext.getEventAdapterService();
    }

    /**
     * Returns the statement's resource handle for locking.
     *
     * @return handle of statement
     */
    public EPStatementHandle getEpStatementHandle() {
        return statementContext.getEpStatementHandle();
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
     * Returns the statement name.
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementContext.getStatementName();
    }

    /**
     * Returns the stream number.
     *
     * @return stream number
     */
    public int getStreamNumber() {
        return streamNumber;
    }

    /**
     * Returns the engine URI.
     *
     * @return engine URI
     */
    public String getEngineURI() {
        return statementContext.getEngineURI();
    }

    /**
     * Returns extension services context for statement (statement-specific).
     *
     * @return extension services
     */
    public StatementExtensionSvcContext getStatementExtensionServicesContext() {
        return statementContext.getStatementExtensionServicesContext();
    }

    /**
     * Returns the variable service.
     *
     * @return variable service
     */
    public VariableService getVariableService() {
        return statementContext.getVariableService();
    }

    public TimeProvider getTimeProvider() {
        return statementContext.getTimeProvider();
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return statementContext.getExceptionHandlingService();
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public MatchedEventMapMeta getMatchedEventMapMeta() {
        return matchedEventMapMeta;
    }

    public boolean isResilient() {
        return isResilient;
    }
}
