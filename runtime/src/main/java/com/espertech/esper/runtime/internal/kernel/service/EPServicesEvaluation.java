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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.internal.context.util.InternalEventRouteDest;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.core.EventTypeResolvingBeanFactory;
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.util.ManagedReadWriteLock;

public interface EPServicesEvaluation {
    FilterService getFilterService();

    ManagedReadWriteLock getEventProcessingRWLock();

    MetricReportingService getMetricReportingService();

    SchedulingService getSchedulingService();

    VariableManagementService getVariableManagementService();

    ExceptionHandlingService getExceptionHandlingService();

    TableExprEvaluatorContext getTableExprEvaluatorContext();

    InternalEventRouteDest getInternalEventRouteDest();

    EventTypeResolvingBeanFactory getEventTypeResolvingBeanFactory();
}
