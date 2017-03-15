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
package com.espertech.esperio.csv;

import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.adapter.AdapterStateManager;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.*;
import com.espertech.esper.epl.metric.StatementMetricHandle;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A skeleton implementation for coordinated adapter reading, for adapters that
 * can do timestamp-coordinated input.
 */
public abstract class AbstractCoordinatedAdapter implements CoordinatedAdapter {
    private static final Logger log = LoggerFactory.getLogger(AbstractCoordinatedAdapter.class);

    /**
     * Statement management.
     */
    protected final AdapterStateManager stateManager = new AdapterStateManager();

    /**
     * Sorted events to be sent.
     */
    protected final SortedSet<SendableEvent> eventsToSend = new TreeSet<SendableEvent>(new SendableEventComparator());

    /**
     * Slot for scheduling.
     */
    protected long scheduleSlot;

    private EPServiceProvider epService;
    private EPRuntime runtime;
    private SchedulingService schedulingService;
    private boolean usingEngineThread, usingExternalTimer, usingTimeSpanEvents;
    private long currentTime = 0;
    private long lastEventTime = 0;
    private long startTime;
    private AbstractSender sender;

    /**
     * Ctor.
     *
     * @param epService           - the EPServiceProvider for the engine runtime and services
     * @param usingEngineThread   - true if the Adapter should set time by the scheduling service in the engine,
     *                            false if it should set time externally through the calling thread
     * @param usingExternalTimer  - true to use esper's external timer mechanism instead of internal timing
     * @param usingTimeSpanEvents - true for time span events
     */
    public AbstractCoordinatedAdapter(EPServiceProvider epService, boolean usingEngineThread, boolean usingExternalTimer, boolean usingTimeSpanEvents) {
        this.usingEngineThread = usingEngineThread;
        this.usingExternalTimer = usingExternalTimer;
        this.usingTimeSpanEvents = usingTimeSpanEvents;

        this.setSender(new DirectSender());
        if (epService == null) {
            return;
        }
        if (!(epService instanceof EPServiceProviderSPI)) {
            throw new IllegalArgumentException("Invalid epService provided");
        }
        this.epService = epService;
        this.runtime = epService.getEPRuntime();
        this.schedulingService = ((EPServiceProviderSPI) epService).getSchedulingService();
    }

    public AdapterState getState() {
        return stateManager.getState();
    }

    public void start() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".start");
        }
        if (runtime == null) {
            throw new EPException("Attempting to start an Adapter that hasn't had the epService provided");
        }
        startTime = getCurrentTime();
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".start startTime==" + startTime);
        }
        stateManager.start();
        sender.setRuntime(runtime);
        continueSendingEvents();
    }

    public void pause() throws EPException {
        stateManager.pause();
    }

    public void resume() throws EPException {
        stateManager.resume();
        continueSendingEvents();
    }

    public void destroy() throws EPException {
        if (sender != null) {
            sender.onFinish();
        }
        stateManager.destroy();
        close();
    }

    public void stop() throws EPException {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".stop");
        }
        stateManager.stop();
        eventsToSend.clear();
        currentTime = 0;
        reset();
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.ReadableAdapter#disallowStateChanges()
     */
    public void disallowStateTransitions() {
        stateManager.disallowStateTransitions();
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.ReadableAdapter#setUsingEngineThread(boolean)
     */
    public void setUsingEngineThread(boolean usingEngineThread) {
        this.usingEngineThread = usingEngineThread;
    }

    /**
     * Set to true to use esper's external timer mechanism instead of internal timing
     *
     * @param usingExternalTimer true for external timer
     */
    public void setUsingExternalTimer(boolean usingExternalTimer) {
        this.usingExternalTimer = usingExternalTimer;
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.CoordinatedAdapter#setScheduleSlot(com.espertech.esper.schedule.ScheduleSlot)
     */
    public void setScheduleSlot(long scheduleSlot) {
        this.scheduleSlot = scheduleSlot;
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.CoordinatedAdapter#setEPService(com.espertech.esper.client.EPServiceProvider)
     */
    public void setEPService(EPServiceProvider epService) {
        if (epService == null) {
            throw new NullPointerException("epService cannot be null");
        }
        if (!(epService instanceof EPServiceProviderSPI)) {
            throw new IllegalArgumentException("Invalid type of EPServiceProvider");
        }
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        runtime = spi.getEPRuntime();
        schedulingService = spi.getSchedulingService();
        sender.setRuntime(runtime);
    }

    /**
     * Perform any actions specific to this Adapter that should
     * be completed before the Adapter is stopped.
     */
    protected abstract void close();

    /**
     * Remove the first member of eventsToSend and insert
     * another event chosen in some fashion specific to this
     * Adapter.
     */
    protected abstract void replaceFirstEventToSend();

    /**
     * Reset all the changeable state of this Adapter, as if it were just created.
     */
    protected abstract void reset();

    private void continueSendingEvents() {
        boolean keepLooping = true;
        while (stateManager.getState() == AdapterState.STARTED && keepLooping) {
            currentTime = getCurrentTime();
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".continueSendingEvents currentTime==" + currentTime);
            }
            fillEventsToSend();
            sendSoonestEvents();
            keepLooping = waitToSendEvents();
        }
    }

    private boolean waitToSendEvents() {
        if (usingExternalTimer) {
            return false;
        } else if (usingEngineThread) {
            scheduleNextCallback();
            return false;
        } else {
            long sleepTime = 0;
            if (eventsToSend.isEmpty()) {
                sleepTime = 100;
            } else {
                sleepTime = eventsToSend.first().getSendTime() - (currentTime - startTime);
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                throw new EPException(ex);
            }
            return true;
        }
    }

    private long getCurrentTime() {
        return usingEngineThread ? schedulingService.getTime() : System.currentTimeMillis();
    }

    private void fillEventsToSend() {
        if (eventsToSend.isEmpty()) {
            SendableEvent theEvent = read();
            if (theEvent != null) {
                eventsToSend.add(theEvent);
            }
        }
    }

    private void sendSoonestEvents() {
        if (usingExternalTimer) {
            // send all events in order and when time clicks over send time event for previous time
            while (!eventsToSend.isEmpty()) {
                long currentEventTime = eventsToSend.first().getSendTime();
                // check whether time has increased. Cannot go backwards due to checks elsewhere
                if (currentEventTime > lastEventTime) {
                    if (this.usingTimeSpanEvents) {
                        this.sender.sendEvent(null, new CurrentTimeSpanEvent(currentEventTime));
                    } else {
                        this.sender.sendEvent(null, new CurrentTimeEvent(currentEventTime));
                    }
                    lastEventTime = currentEventTime;
                }
                sendFirstEvent();
            }
        } else {
            // watch time and send events to catch up
            while (!eventsToSend.isEmpty() && eventsToSend.first().getSendTime() <= currentTime - startTime) {
                sendFirstEvent();
            }
        }
    }

    private void sendFirstEvent() {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".sendFirstEvent currentTime==" + currentTime);
            log.debug(".sendFirstEvent sending event " + eventsToSend.first() + ", its sendTime==" + eventsToSend.first().getSendTime());
        }
        sender.setRuntime(runtime);
        eventsToSend.first().send(sender);
        replaceFirstEventToSend();
    }

    private void scheduleNextCallback() {
        ScheduleHandleCallback nextScheduleCallback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                continueSendingEvents();
            }
        };
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        StatementMetricHandle metricsHandle = spi.getMetricReportingService().getStatementHandle(-1, "AbstractCoordinatedAdapter");
        EPStatementHandle stmtHandle = new EPStatementHandle(-1, "AbstractCoordinatedAdapter", null, StatementType.ESPERIO, "AbstractCoordinatedAdapter", false, metricsHandle, 0, false, false, spi.getServicesContext().getMultiMatchHandlerFactory().getDefaultHandler());
        EPStatementAgentInstanceHandle agentInstanceHandle = new EPStatementAgentInstanceHandle(stmtHandle, new StatementAgentInstanceRWLockImpl(false), -1, new StatementAgentInstanceFilterVersion(), null);
        EPStatementHandleCallback scheduleCSVHandle = new EPStatementHandleCallback(agentInstanceHandle, nextScheduleCallback);
        long nextScheduleSlot;

        if (eventsToSend.isEmpty()) {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".scheduleNextCallback no events to send, scheduling callback in 100 ms");
            }
            nextScheduleSlot = 0L;
            schedulingService.add(100, scheduleCSVHandle, nextScheduleSlot);
        } else {
            // Offset is not a function of the currentTime alone.
            long baseMsec = currentTime - startTime;
            long afterMsec = eventsToSend.first().getSendTime() - baseMsec;

            nextScheduleSlot = eventsToSend.first().getScheduleSlot();
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".scheduleNextCallback schedulingCallback in " + afterMsec + " milliseconds");
            }
            schedulingService.add(afterMsec, scheduleCSVHandle, nextScheduleSlot);
        }
    }

    /**
     * Returns the runtime.
     *
     * @return runtime
     */
    public EPRuntime getRuntime() {
        return runtime;
    }

    /**
     * Sets a new sender to use.
     *
     * @param sender for sending
     */
    public void setSender(AbstractSender sender) {
        this.sender = sender;
        this.sender.setRuntime(this.runtime);
    }
}
