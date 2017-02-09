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
package com.espertech.esper.core.support;

import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.schedule.ScheduleBucket;
import com.espertech.esper.schedule.ScheduleHandle;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.SchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SupportSchedulingServiceImpl implements SchedulingService {
    private Map<Long, ScheduleHandle> added = new HashMap<Long, ScheduleHandle>();
    private long currentTime;

    public Map<Long, ScheduleHandle> getAdded() {
        return added;
    }

    public void evaluateLock() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evaluateUnLock() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void add(long afterTime, ScheduleHandle callback, long scheduleSlot) {
        log.debug(".add Not implemented, afterMSec=" + afterTime + " callback=" + callback.getClass().getName());
        added.put(afterTime, callback);
    }

    public void remove(ScheduleHandle callback, long scheduleSlot) {
        log.debug(".remove Not implemented, callback=" + callback.getClass().getName());
    }

    public long getTime() {
        log.debug(".getTime Time is " + currentTime);
        return this.currentTime;
    }

    public void setTime(long currentTime) {
        log.debug(".setTime Setting new time, currentTime=" + currentTime);
        this.currentTime = currentTime;
    }

    public void evaluate(Collection<ScheduleHandle> handles) {
        log.debug(".evaluate Not implemented");
    }

    public ScheduleBucket allocateBucket() {
        return new ScheduleBucket(0);
    }

    public static void evaluateSchedule(SchedulingService service) {
        Collection<ScheduleHandle> handles = new LinkedList<ScheduleHandle>();
        service.evaluate(handles);

        for (ScheduleHandle handle : handles) {
            if (handle instanceof EPStatementHandleCallback) {
                EPStatementHandleCallback callback = (EPStatementHandleCallback) handle;
                callback.getScheduleCallback().scheduledTrigger(null);
            } else {
                ScheduleHandleCallback cb = (ScheduleHandleCallback) handle;
                cb.scheduledTrigger(null);
            }
        }
    }

    public void destroy() {
    }

    public int getTimeHandleCount() {
        throw new RuntimeException("not implemented");
    }

    public Long getFurthestTimeHandle() {
        throw new RuntimeException("not implemented");
    }

    public int getScheduleHandleCount() {
        throw new RuntimeException("not implemented");
    }

    public boolean isScheduled(ScheduleHandle scheduleHandle) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private static final Logger log = LoggerFactory.getLogger(SupportSchedulingServiceImpl.class);
}
