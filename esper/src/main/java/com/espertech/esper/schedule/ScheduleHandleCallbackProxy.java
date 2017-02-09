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
package com.espertech.esper.schedule;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.lang.reflect.Method;

public class ScheduleHandleCallbackProxy implements java.lang.reflect.InvocationHandler {

    private static Method target = JavaClassHelper.getMethodByName(ScheduleHandleCallback.class, "scheduledTrigger");

    private final String engineURI;
    private final String statementName;
    private final ScheduleHandleCallback scheduleHandleCallback;

    public static Object newInstance(String engineURI, String statementName, ScheduleHandleCallback scheduleHandleCallback) {
        return java.lang.reflect.Proxy.newProxyInstance(
                scheduleHandleCallback.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(scheduleHandleCallback.getClass()),
                new ScheduleHandleCallbackProxy(engineURI, statementName, scheduleHandleCallback));
    }

    public ScheduleHandleCallbackProxy(String engineURI, String statementName, ScheduleHandleCallback scheduleHandleCallback) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.scheduleHandleCallback = scheduleHandleCallback;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        if (m.getName().equals(target.getName())) {
            if (AuditPath.isInfoEnabled()) {
                StringWriter message = new StringWriter();
                message.write("trigger handle ");
                JavaClassHelper.writeInstance(message, scheduleHandleCallback, true);
                AuditPath.auditLog(engineURI, statementName, AuditEnum.SCHEDULE, message.toString());
            }
        }

        return m.invoke(scheduleHandleCallback, args);
    }
}

