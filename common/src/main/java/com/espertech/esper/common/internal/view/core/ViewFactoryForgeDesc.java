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

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;

import java.util.List;

public class ViewFactoryForgeDesc {

    private final List<ViewFactoryForge> forges;
    private final List<StmtClassForgeableFactory> multikeyForges;
    private final List<ScheduleHandleTracked> schedules;
    private final FabricCharge fabricCharge;

    public ViewFactoryForgeDesc(List<ViewFactoryForge> forges, List<StmtClassForgeableFactory> multikeyForges, List<ScheduleHandleTracked> schedules, FabricCharge fabricCharge) {
        this.forges = forges;
        this.multikeyForges = multikeyForges;
        this.schedules = schedules;
        this.fabricCharge = fabricCharge;
    }

    public List<ViewFactoryForge> getForges() {
        return forges;
    }

    public List<StmtClassForgeableFactory> getMultikeyForges() {
        return multikeyForges;
    }

    public List<ScheduleHandleTracked> getSchedules() {
        return schedules;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
