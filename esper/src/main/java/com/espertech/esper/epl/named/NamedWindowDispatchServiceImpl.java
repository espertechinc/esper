/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.ExceptionHandlingService;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.metric.MetricReportingPath;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.util.ManagedReadWriteLock;
import com.espertech.esper.util.MetricUtil;

import java.util.*;

/**
 * This service hold for each named window a dedicated processor and a lock to the named window.
 * This lock is shrared between the named window and on-delete statements.
 */
public class NamedWindowDispatchServiceImpl implements NamedWindowDispatchService
{
    private final SchedulingService schedulingService;
    private final VariableService variableService;
    private final TableService tableService;
    private final ExceptionHandlingService exceptionHandlingService;
    private final boolean isPrioritized;
    private final ManagedReadWriteLock eventProcessingRWLock;
    private final MetricReportingService metricReportingService;

    private ThreadLocal<List<NamedWindowConsumerDispatchUnit>> threadLocal = new ThreadLocal<List<NamedWindowConsumerDispatchUnit>>()
    {
        protected synchronized List<NamedWindowConsumerDispatchUnit> initialValue()
        {
            return new ArrayList<NamedWindowConsumerDispatchUnit>();
        }
    };

    private ThreadLocal<Map<EPStatementAgentInstanceHandle, Object>> dispatchesPerStmtTL = new ThreadLocal<Map<EPStatementAgentInstanceHandle, Object>>()
    {
        protected synchronized Map<EPStatementAgentInstanceHandle, Object> initialValue()
        {
            return new HashMap<EPStatementAgentInstanceHandle, Object>();
        }
    };

    /**
     * Ctor.
     * @param variableService is for variable access
     * @param isPrioritized if the engine is running with prioritized execution
     */
    public NamedWindowDispatchServiceImpl(SchedulingService schedulingService,
                                          VariableService variableService,
                                          TableService tableService,
                                          boolean isPrioritized,
                                          ManagedReadWriteLock eventProcessingRWLock,
                                          ExceptionHandlingService exceptionHandlingService,
                                          MetricReportingService metricReportingService)
    {
        this.schedulingService = schedulingService;
        this.variableService = variableService;
        this.tableService = tableService;
        this.isPrioritized = isPrioritized;
        this.eventProcessingRWLock = eventProcessingRWLock;
        this.exceptionHandlingService = exceptionHandlingService;
        this.metricReportingService = metricReportingService;
    }

    public NamedWindowTailView createTailView(EventType eventType, NamedWindowMgmtService namedWindowMgmtService, NamedWindowDispatchService namedWindowDispatchService, StatementResultService statementResultService, ValueAddEventProcessor revisionProcessor, boolean prioritized, boolean parentBatchWindow) {
        return new NamedWindowTailView(eventType, namedWindowMgmtService, namedWindowDispatchService, statementResultService, revisionProcessor, isPrioritized, parentBatchWindow);
    }

    public void destroy()
    {
        threadLocal.remove();
        dispatchesPerStmtTL.remove();
    }

    public void addDispatch(NamedWindowDeltaData delta, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumers)
    {
        NamedWindowConsumerDispatchUnit unit = new NamedWindowConsumerDispatchUnit(delta, consumers);
        threadLocal.get().add(unit);
    }

    public boolean dispatch()
    {
        List<NamedWindowConsumerDispatchUnit> dispatches = threadLocal.get();
        if (dispatches.isEmpty())
        {
            return false;
        }

        while (!dispatches.isEmpty()) {

            // Acquire main processing lock which locks out statement management
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qNamedWindowDispatch(exceptionHandlingService.getEngineURI());}
            eventProcessingRWLock.acquireReadLock();
            try
            {
                NamedWindowConsumerDispatchUnit[] units = dispatches.toArray(new NamedWindowConsumerDispatchUnit[dispatches.size()]);
                dispatches.clear();
                processDispatches(units);
            }
            catch (RuntimeException ex)
            {
                throw new EPException(ex);
            }
            finally
            {
                eventProcessingRWLock.releaseReadLock();
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aNamedWindowDispatch();}
            }
        }

        return true;
    }

    private void processDispatches(NamedWindowConsumerDispatchUnit[] dispatches) {

        if (dispatches.length == 1)
        {
            NamedWindowConsumerDispatchUnit unit = dispatches[0];
            EventBean[] newData = unit.getDeltaData().getNewData();
            EventBean[] oldData = unit.getDeltaData().getOldData();

            if (MetricReportingPath.isMetricsEnabled)
            {
                for (Map.Entry<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> entry : unit.getDispatchTo().entrySet())
                {
                    EPStatementAgentInstanceHandle handle = entry.getKey();
                    if (handle.getStatementHandle().getMetricsHandle().isEnabled()) {
                        long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                        long wallTimeBefore = MetricUtil.getWall();

                        processHandle(handle, entry.getValue(), newData, oldData);

                        long wallTimeAfter = MetricUtil.getWall();
                        long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                        long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                        long deltaWall = wallTimeAfter - wallTimeBefore;
                        metricReportingService.accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
                    }
                    else {
                        processHandle(handle, entry.getValue(), newData, oldData);
                    }

                    if ((isPrioritized) && (handle.isPreemptive()))
                    {
                        break;
                    }
                }
            }
            else {
                for (Map.Entry<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> entry : unit.getDispatchTo().entrySet())
                {
                    EPStatementAgentInstanceHandle handle = entry.getKey();
                    processHandle(handle, entry.getValue(), newData, oldData);

                    if ((isPrioritized) && (handle.isPreemptive()))
                    {
                        break;
                    }
                }
            }

            return;
        }

        // Multiple different-result dispatches to same or different statements are needed in two situations:
        // a) an event comes in, triggers two insert-into statements inserting into the same named window and the window produces 2 results
        // b) a time batch is grouped in the named window, and a timer fires for both groups at the same time producing more then one result
        // c) two on-merge/update/delete statements fire for the same arriving event each updating the named window

        // Most likely all dispatches go to different statements since most statements are not joins of
        // named windows that produce results at the same time. Therefore sort by statement handle.
        Map<EPStatementAgentInstanceHandle, Object> dispatchesPerStmt = dispatchesPerStmtTL.get();
        for (NamedWindowConsumerDispatchUnit unit : dispatches)
        {
            for (Map.Entry<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> entry : unit.getDispatchTo().entrySet())
            {
                EPStatementAgentInstanceHandle handle = entry.getKey();
                Object perStmtObj = dispatchesPerStmt.get(handle);
                if (perStmtObj == null)
                {
                    dispatchesPerStmt.put(handle, unit);
                }
                else if (perStmtObj instanceof List)
                {
                    List<NamedWindowConsumerDispatchUnit> list = (List<NamedWindowConsumerDispatchUnit>) perStmtObj;
                    list.add(unit);
                }
                else    // convert from object to list
                {
                    NamedWindowConsumerDispatchUnit unitObj = (NamedWindowConsumerDispatchUnit) perStmtObj;
                    List<NamedWindowConsumerDispatchUnit> list = new ArrayList<NamedWindowConsumerDispatchUnit>();
                    list.add(unitObj);
                    list.add(unit);
                    dispatchesPerStmt.put(handle, list);
                }
            }
        }

        // Dispatch - with or without metrics reporting
        if (MetricReportingPath.isMetricsEnabled)
        {
            for (Map.Entry<EPStatementAgentInstanceHandle, Object> entry : dispatchesPerStmt.entrySet())
            {
                EPStatementAgentInstanceHandle handle = entry.getKey();
                Object perStmtObj = entry.getValue();

                // dispatch of a single result to the statement
                if (perStmtObj instanceof NamedWindowConsumerDispatchUnit)
                {
                    NamedWindowConsumerDispatchUnit unit = (NamedWindowConsumerDispatchUnit) perStmtObj;
                    EventBean[] newData = unit.getDeltaData().getNewData();
                    EventBean[] oldData = unit.getDeltaData().getOldData();

                    if (handle.getStatementHandle().getMetricsHandle().isEnabled()) {
                        long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                        long wallTimeBefore = MetricUtil.getWall();

                        processHandle(handle, unit.getDispatchTo().get(handle), newData, oldData);

                        long wallTimeAfter = MetricUtil.getWall();
                        long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                        long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                        long deltaWall = wallTimeAfter - wallTimeBefore;
                        metricReportingService.accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
                    }
                    else {
                        Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> entries = unit.getDispatchTo();
                    	List<NamedWindowConsumerView> items = entries.get(handle);
                    	if (items != null) {
							processHandle(handle, items, newData, oldData);
						}
                    }

                    if ((isPrioritized) && (handle.isPreemptive()))
                    {
                        break;
                    }

                    continue;
                }

                // dispatch of multiple results to a the same statement, need to aggregate per consumer view
                LinkedHashMap<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer = getDeltaPerConsumer(perStmtObj, handle);
                if (handle.getStatementHandle().getMetricsHandle().isEnabled()) {
                    long cpuTimeBefore = MetricUtil.getCPUCurrentThread();
                    long wallTimeBefore = MetricUtil.getWall();

                    processHandleMultiple(handle, deltaPerConsumer);

                    long wallTimeAfter = MetricUtil.getWall();
                    long cpuTimeAfter = MetricUtil.getCPUCurrentThread();
                    long deltaCPU = cpuTimeAfter - cpuTimeBefore;
                    long deltaWall = wallTimeAfter - wallTimeBefore;
                    metricReportingService.accountTime(handle.getStatementHandle().getMetricsHandle(), deltaCPU, deltaWall, 1);
                }
                else {
                    processHandleMultiple(handle, deltaPerConsumer);
                }

                if ((isPrioritized) && (handle.isPreemptive()))
                {
                    break;
                }
            }
        }
        else {

            for (Map.Entry<EPStatementAgentInstanceHandle, Object> entry : dispatchesPerStmt.entrySet())
            {
                EPStatementAgentInstanceHandle handle = entry.getKey();
                Object perStmtObj = entry.getValue();

                // dispatch of a single result to the statement
                if (perStmtObj instanceof NamedWindowConsumerDispatchUnit)
                {
                    NamedWindowConsumerDispatchUnit unit = (NamedWindowConsumerDispatchUnit) perStmtObj;
                    EventBean[] newData = unit.getDeltaData().getNewData();
                    EventBean[] oldData = unit.getDeltaData().getOldData();

                    processHandle(handle, unit.getDispatchTo().get(handle), newData, oldData);

                    if ((isPrioritized) && (handle.isPreemptive()))
                    {
                        break;
                    }

                    continue;
                }

                // dispatch of multiple results to a the same statement, need to aggregate per consumer view
                LinkedHashMap<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer = getDeltaPerConsumer(perStmtObj, handle);
                processHandleMultiple(handle, deltaPerConsumer);

                if ((isPrioritized) && (handle.isPreemptive()))
                {
                    break;
                }
            }
        }

        dispatchesPerStmt.clear();
    }

    private void processHandleMultiple(EPStatementAgentInstanceHandle handle, Map<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer) {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qNamedWindowCPMulti(exceptionHandlingService.getEngineURI(), deltaPerConsumer, handle, schedulingService.getTime());}
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try
        {
            if (handle.isHasVariables())
            {
                variableService.setLocalVersion();
            }
            for (Map.Entry<NamedWindowConsumerView, NamedWindowDeltaData> entryDelta : deltaPerConsumer.entrySet())
            {
                EventBean[] newData = entryDelta.getValue().getNewData();
                EventBean[] oldData = entryDelta.getValue().getOldData();
                entryDelta.getKey().update(newData, oldData);
            }

            // internal join processing, if applicable
            handle.internalDispatch();
        }
        catch (RuntimeException ex) {
            exceptionHandlingService.handleException(ex, handle);
        }
        finally
        {
            if (handle.isHasTableAccess()) {
                tableService.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aNamedWindowCPMulti();}
        }
    }

    private void processHandle(EPStatementAgentInstanceHandle handle, List<NamedWindowConsumerView> value, EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qNamedWindowCPSingle(exceptionHandlingService.getEngineURI(), value, newData, oldData, handle, schedulingService.getTime());}
        handle.getStatementAgentInstanceLock().acquireWriteLock();
        try
        {
            if (handle.isHasVariables())
            {
                variableService.setLocalVersion();
            }

            for (NamedWindowConsumerView consumerView : value)
            {
                consumerView.update(newData, oldData);
            }

            // internal join processing, if applicable
            handle.internalDispatch();
        }
        catch (RuntimeException ex) {
            exceptionHandlingService.handleException(ex, handle);
        }
        finally
        {
            if (handle.isHasTableAccess()) {
                tableService.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            handle.getStatementAgentInstanceLock().releaseWriteLock();
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aNamedWindowCPSingle();}
        }
    }

    public LinkedHashMap<NamedWindowConsumerView, NamedWindowDeltaData> getDeltaPerConsumer(Object perStmtObj, EPStatementAgentInstanceHandle handle) {
        List<NamedWindowConsumerDispatchUnit> list = (List<NamedWindowConsumerDispatchUnit>) perStmtObj;
        LinkedHashMap<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer = new LinkedHashMap<NamedWindowConsumerView, NamedWindowDeltaData>();
        for (NamedWindowConsumerDispatchUnit unit : list)   // for each unit
        {
            for (NamedWindowConsumerView consumerView : unit.getDispatchTo().get(handle))   // each consumer
            {
                NamedWindowDeltaData deltaForConsumer = deltaPerConsumer.get(consumerView);
                if (deltaForConsumer == null)
                {
                    deltaPerConsumer.put(consumerView, unit.getDeltaData());
                }
                else
                {
                    NamedWindowDeltaData aggregated = new NamedWindowDeltaData(deltaForConsumer, unit.getDeltaData());
                    deltaPerConsumer.put(consumerView, aggregated);
                }
            }
        }
        return deltaPerConsumer;
    }
}
