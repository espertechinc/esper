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
package com.espertech.esper.core.service;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.schedule.*;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;

public class SchedulingServiceAudit implements SchedulingServiceSPI {

    private final String engineUri;
    private final String statementName;
    private final SchedulingServiceSPI spi;

    public SchedulingServiceAudit(String engineUri, String statementName, SchedulingServiceSPI spi) {
        this.engineUri = engineUri;
        this.statementName = statementName;
        this.spi = spi;
    }

    public boolean isScheduled(ScheduleHandle handle) {
        return spi.isScheduled(handle);
    }

    public ScheduleSet take(Set<Integer> statementId) {
        return spi.take(statementId);
    }

    public void apply(ScheduleSet scheduleSet) {
        spi.apply(scheduleSet);
    }

    public Long getNearestTimeHandle() {
        return spi.getNearestTimeHandle();
    }

    public void visitSchedules(ScheduleVisitor visitor) {
        spi.visitSchedules(visitor);
    }

    public void add(long afterMSec, ScheduleHandle handle, long slot) throws ScheduleServiceException {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.write("after ");
            message.write(Long.toString(afterMSec));
            message.write(" handle ");
            printHandle(message, handle);

            AuditPath.auditLog(engineUri, statementName, AuditEnum.SCHEDULE, message.toString());

            modifyCreateProxy(handle);
        }
        spi.add(afterMSec, handle, slot);
    }

    public void remove(ScheduleHandle handle, long slot) throws ScheduleServiceException {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.write("remove handle ");
            printHandle(message, handle);

            AuditPath.auditLog(engineUri, statementName, AuditEnum.SCHEDULE, message.toString());
        }
        spi.remove(handle, slot);
    }

    public void setTime(long timestamp) {
        spi.setTime(timestamp);
    }

    public void evaluate(Collection<ScheduleHandle> handles) {
        spi.evaluate(handles);
    }

    public void destroy() {
        spi.destroy();
    }

    public int getTimeHandleCount() {
        return spi.getTimeHandleCount();
    }

    public Long getFurthestTimeHandle() {
        return spi.getFurthestTimeHandle();
    }

    public int getScheduleHandleCount() {
        return spi.getScheduleHandleCount();
    }

    public long getTime() {
        return spi.getTime();
    }

    public void init() {
        // no action required
    }

    private void printHandle(StringWriter message, ScheduleHandle handle) {
        if (handle instanceof EPStatementHandleCallback) {
            EPStatementHandleCallback callback = (EPStatementHandleCallback) handle;
            JavaClassHelper.writeInstance(message, callback.getScheduleCallback(), true);
        } else {
            JavaClassHelper.writeInstance(message, handle, true);
        }
    }

    private void modifyCreateProxy(ScheduleHandle handle) {
        if (!(handle instanceof EPStatementHandleCallback)) {
            return;
        }
        EPStatementHandleCallback callback = (EPStatementHandleCallback) handle;
        ScheduleHandleCallback sc = (ScheduleHandleCallback) ScheduleHandleCallbackProxy.newInstance(engineUri, statementName, callback.getScheduleCallback());
        callback.setScheduleCallback(sc);
    }
}
