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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecOptions;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

import java.lang.annotation.Annotation;

public class ViewFactoryForgeArgs {
    private final StatementRawInfo statementRawInfo;
    private final int streamNum;
    private final StreamSpecOptions options;
    private final boolean isSubquery;
    private final int subqueryNumber;
    private final String optionalCreateNamedWindowName;
    private final StatementCompileTimeServices compileTimeServices;

    public ViewFactoryForgeArgs(int streamNum, boolean isSubquery, int subqueryNumber, StreamSpecOptions options, String optionalCreateNamedWindowName, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        this.statementRawInfo = statementRawInfo;
        this.streamNum = streamNum;
        this.options = options;
        this.isSubquery = isSubquery;
        this.subqueryNumber = subqueryNumber;
        this.optionalCreateNamedWindowName = optionalCreateNamedWindowName;
        this.compileTimeServices = compileTimeServices;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public StreamSpecOptions getOptions() {
        return options;
    }

    public boolean isSubquery() {
        return isSubquery;
    }

    public int getSubqueryNumber() {
        return subqueryNumber;
    }

    public ClasspathImportServiceCompileTime getClasspathImportService() {
        return compileTimeServices.getClasspathImportServiceCompileTime();
    }

    public Configuration getConfiguration() {
        return compileTimeServices.getConfiguration();
    }

    public ViewResolutionService getViewResolutionService() {
        return compileTimeServices.getViewResolutionService();
    }

    public BeanEventTypeFactory getBeanEventTypeFactoryPrivate() {
        return compileTimeServices.getBeanEventTypeFactoryPrivate();
    }

    public EventTypeCompileTimeRegistry getEventTypeModuleCompileTimeRegistry() {
        return compileTimeServices.getEventTypeCompileTimeRegistry();
    }

    public Annotation[] getAnnotations() {
        return statementRawInfo.getAnnotations();
    }

    public String getStatementName() {
        return statementRawInfo.getStatementName();
    }

    public int getStatementNumber() {
        return statementRawInfo.getStatementNumber();
    }

    public StatementCompileTimeServices getCompileTimeServices() {
        return compileTimeServices;
    }

    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }

    public String getOptionalCreateNamedWindowName() {
        return optionalCreateNamedWindowName;
    }
}
