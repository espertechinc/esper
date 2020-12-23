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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecTracked;
import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;

import java.util.List;

public class StmtForgeMethodResult {

    private final List<StmtClassForgeable> forgeables;
    private final List<FilterSpecTracked> filtereds;
    private final List<ScheduleHandleTracked> scheduleds;
    private final List<NamedWindowConsumerStreamSpec> namedWindowConsumers;
    private final List<FilterSpecParamExprNodeForge> filterBooleanExpressions;
    private final CodegenPackageScope packageScope;
    private final FabricCharge fabricCharge;

    public StmtForgeMethodResult(List<StmtClassForgeable> forgeables, List<FilterSpecTracked> filtereds, List<ScheduleHandleTracked> scheduleds, List<NamedWindowConsumerStreamSpec> namedWindowConsumers, List<FilterSpecParamExprNodeForge> filterBooleanExpressions, CodegenPackageScope packageScope, FabricCharge fabricCharge) {
        this.forgeables = forgeables;
        this.filtereds = filtereds;
        this.scheduleds = scheduleds;
        this.namedWindowConsumers = namedWindowConsumers;
        this.filterBooleanExpressions = filterBooleanExpressions;
        this.packageScope = packageScope;
        this.fabricCharge = fabricCharge;
    }

    public List<StmtClassForgeable> getForgeables() {
        return forgeables;
    }

    public List<ScheduleHandleTracked> getScheduleds() {
        return scheduleds;
    }

    public List<FilterSpecTracked> getFiltereds() {
        return filtereds;
    }

    public List<NamedWindowConsumerStreamSpec> getNamedWindowConsumers() {
        return namedWindowConsumers;
    }

    public List<FilterSpecParamExprNodeForge> getFilterBooleanExpressions() {
        return filterBooleanExpressions;
    }

    public CodegenPackageScope getPackageScope() {
        return packageScope;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
