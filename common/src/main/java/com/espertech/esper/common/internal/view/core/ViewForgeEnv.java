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
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventTypeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

import java.lang.annotation.Annotation;

public class ViewForgeEnv {
    private final ViewFactoryForgeArgs args;

    public ViewForgeEnv(ViewFactoryForgeArgs args) {
        this.args = args;
    }

    public ClasspathImportServiceCompileTime getClasspathImportServiceCompileTime() {
        return args.getClasspathImportService();
    }

    public Configuration getConfiguration() {
        return args.getConfiguration();
    }

    public BeanEventTypeFactory getBeanEventTypeFactoryProtected() {
        return args.getBeanEventTypeFactoryPrivate();
    }

    public EventTypeCompileTimeRegistry getEventTypeModuleCompileTimeRegistry() {
        return args.getEventTypeModuleCompileTimeRegistry();
    }

    public Annotation[] getAnnotations() {
        return args.getAnnotations();
    }

    public String getOptionalStatementName() {
        return args.getStatementName();
    }

    public int getStatementNumber() {
        return args.getStatementNumber();
    }

    public StatementCompileTimeServices getStatementCompileTimeServices() {
        return args.getCompileTimeServices();
    }

    public StatementRawInfo getStatementRawInfo() {
        return args.getStatementRawInfo();
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return args.getCompileTimeServices().getVariableCompileTimeResolver();
    }

    public String getContextName() {
        return args.getStatementRawInfo().getContextName();
    }

    public EventTypeCompileTimeResolver getEventTypeCompileTimeResolver() {
        return args.getCompileTimeServices().getEventTypeCompileTimeResolver();
    }

    public String getModuleName() {
        return args.getStatementRawInfo().getModuleName();
    }

    public SerdeEventTypeCompileTimeRegistry getSerdeEventTypeRegistry() {
        return args.getCompileTimeServices().getSerdeEventTypeRegistry();
    }

    public SerdeCompileTimeResolver getSerdeResolver() {
        return args.getCompileTimeServices().getSerdeResolver();
    }
}
