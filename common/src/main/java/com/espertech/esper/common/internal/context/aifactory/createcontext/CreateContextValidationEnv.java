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
package com.espertech.esper.common.internal.context.aifactory.createcontext;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecTracked;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;

import java.util.List;

public class CreateContextValidationEnv {

    private final String contextName;
    private final NameAccessModifier contextVisibility;
    private final StatementRawInfo statementRawInfo;
    private final StatementCompileTimeServices services;
    private final List<FilterSpecTracked> filterSpecCompileds;
    private final List<ScheduleHandleTracked> scheduleHandleCallbackProviders;
    private final List<FilterSpecParamExprNodeForge> filterBooleanExpressions;

    public CreateContextValidationEnv(String contextName, NameAccessModifier contextVisibility, StatementRawInfo statementRawInfo, StatementCompileTimeServices services, List<FilterSpecTracked> filterSpecCompileds, List<ScheduleHandleTracked> scheduleHandleCallbackProviders, List<FilterSpecParamExprNodeForge> filterBooleanExpressions) {
        this.contextName = contextName;
        this.contextVisibility = contextVisibility;
        this.statementRawInfo = statementRawInfo;
        this.services = services;
        this.filterSpecCompileds = filterSpecCompileds;
        this.scheduleHandleCallbackProviders = scheduleHandleCallbackProviders;
        this.filterBooleanExpressions = filterBooleanExpressions;
    }

    public String getContextName() {
        return contextName;
    }

    public NameAccessModifier getContextVisibility() {
        return contextVisibility;
    }

    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }

    public StatementCompileTimeServices getServices() {
        return services;
    }

    public List<FilterSpecTracked> getFilterSpecCompileds() {
        return filterSpecCompileds;
    }

    public List<ScheduleHandleTracked> getScheduleHandleCallbackProviders() {
        return scheduleHandleCallbackProviders;
    }

    public List<FilterSpecParamExprNodeForge> getFilterBooleanExpressions() {
        return filterBooleanExpressions;
    }
}
