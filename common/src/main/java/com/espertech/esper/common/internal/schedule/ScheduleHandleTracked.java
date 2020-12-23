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
package com.espertech.esper.common.internal.schedule;

import com.espertech.esper.common.internal.compile.util.CallbackAttribution;

public class ScheduleHandleTracked {
    private final CallbackAttribution attribution;
    private final ScheduleHandleCallbackProvider provider;

    public ScheduleHandleTracked(CallbackAttribution attribution, ScheduleHandleCallbackProvider provider) {
        this.attribution = attribution;
        this.provider = provider;
    }

    public CallbackAttribution getAttribution() {
        return attribution;
    }

    public ScheduleHandleCallbackProvider getProvider() {
        return provider;
    }
}
