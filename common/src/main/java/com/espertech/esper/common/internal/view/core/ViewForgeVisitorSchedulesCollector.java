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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

public class ViewForgeVisitorSchedulesCollector implements ViewForgeVisitor {
    private final List<ScheduleHandleCallbackProvider> providers;

    public ViewForgeVisitorSchedulesCollector(List<ScheduleHandleCallbackProvider> providers) {
        this.providers = providers;
    }

    public void visit(ViewFactoryForge forge) {
        if (forge instanceof ScheduleHandleCallbackProvider) {
            providers.add((ScheduleHandleCallbackProvider) forge);
        }
    }
}
