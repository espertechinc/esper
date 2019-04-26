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

import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

public class StmtForgeMethodResult {

    private final List<StmtClassForgeable> forgeables;
    private final List<FilterSpecCompiled> filtereds;
    private final List<ScheduleHandleCallbackProvider> scheduleds;
    private final List<NamedWindowConsumerStreamSpec> namedWindowConsumers;
    private final List<FilterSpecParamExprNodeForge> filterBooleanExpressions;

    public StmtForgeMethodResult(List<StmtClassForgeable> forgeables, List<FilterSpecCompiled> filtereds, List<ScheduleHandleCallbackProvider> scheduleds, List<NamedWindowConsumerStreamSpec> namedWindowConsumers, List<FilterSpecParamExprNodeForge> filterBooleanExpressions) {
        this.forgeables = forgeables;
        this.filtereds = filtereds;
        this.scheduleds = scheduleds;
        this.namedWindowConsumers = namedWindowConsumers;
        this.filterBooleanExpressions = filterBooleanExpressions;
    }

    public List<StmtClassForgeable> getForgeables() {
        return forgeables;
    }

    public List<ScheduleHandleCallbackProvider> getScheduleds() {
        return scheduleds;
    }

    public List<FilterSpecCompiled> getFiltereds() {
        return filtereds;
    }

    public List<NamedWindowConsumerStreamSpec> getNamedWindowConsumers() {
        return namedWindowConsumers;
    }

    public List<FilterSpecParamExprNodeForge> getFilterBooleanExpressions() {
        return filterBooleanExpressions;
    }
}
