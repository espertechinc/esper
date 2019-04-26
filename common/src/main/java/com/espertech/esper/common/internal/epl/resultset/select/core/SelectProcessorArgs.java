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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.compile.stage1.spec.ForClauseSpec;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCompileTimeResolver;
import com.espertech.esper.common.internal.epl.resultset.core.GroupByRollupInfo;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventTypeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

import java.lang.annotation.Annotation;

public class SelectProcessorArgs {
    private final SelectClauseElementCompiled[] selectionList;
    private final boolean isUsingWildcard;
    private EventType optionalInsertIntoEventType;
    private final ForClauseSpec forClauseSpec;
    private final StreamTypeService typeService;
    private final ContextCompileTimeDescriptor contextDescriptor;
    private final boolean isFireAndForget;
    private final Annotation[] annotations;
    private final GroupByRollupInfo groupByRollupInfo;
    private final StatementRawInfo statementRawInfo;
    private final StatementCompileTimeServices compileTimeServices;

    public SelectProcessorArgs(SelectClauseElementCompiled[] selectionList, GroupByRollupInfo groupByRollupInfo, boolean isUsingWildcard, EventType optionalInsertIntoEventType, ForClauseSpec forClauseSpec, StreamTypeService typeService, ContextCompileTimeDescriptor contextDescriptor, boolean isFireAndForget, Annotation[] annotations, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        this.selectionList = selectionList;
        this.groupByRollupInfo = groupByRollupInfo;
        this.isUsingWildcard = isUsingWildcard;
        this.optionalInsertIntoEventType = optionalInsertIntoEventType;
        this.forClauseSpec = forClauseSpec;
        this.typeService = typeService;
        this.contextDescriptor = contextDescriptor;
        this.isFireAndForget = isFireAndForget;
        this.annotations = annotations;
        this.statementRawInfo = statementRawInfo;
        this.compileTimeServices = compileTimeServices;
    }

    public SelectClauseElementCompiled[] getSelectionList() {
        return selectionList;
    }

    public boolean isUsingWildcard() {
        return isUsingWildcard;
    }

    public EventType getOptionalInsertIntoEventType() {
        return optionalInsertIntoEventType;
    }

    public ForClauseSpec getForClauseSpec() {
        return forClauseSpec;
    }

    public StreamTypeService getTypeService() {
        return typeService;
    }

    public ClasspathImportServiceCompileTime getClasspathImportService() {
        return compileTimeServices.getClasspathImportServiceCompileTime();
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return compileTimeServices.getVariableCompileTimeResolver();
    }

    public String getStatementName() {
        return statementRawInfo.getStatementName();
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public ContextCompileTimeDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    public Configuration getConfiguration() {
        return compileTimeServices.getConfiguration();
    }

    public EventTypeCompileTimeRegistry getEventTypeCompileTimeRegistry() {
        return compileTimeServices.getEventTypeCompileTimeRegistry();
    }

    public int getStatementNumber() {
        return statementRawInfo.getStatementNumber();
    }

    public BeanEventTypeFactoryPrivate getBeanEventTypeFactoryPrivate() {
        return compileTimeServices.getBeanEventTypeFactoryPrivate();
    }

    public StatementCompileTimeServices getCompileTimeServices() {
        return compileTimeServices;
    }

    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }

    public NamedWindowCompileTimeResolver getNamedWindowCompileTimeResolver() {
        return compileTimeServices.getNamedWindowCompileTimeResolver();
    }

    public TableCompileTimeResolver getTableCompileTimeResolver() {
        return compileTimeServices.getTableCompileTimeResolver();
    }

    public void setOptionalInsertIntoEventType(EventType optionalInsertIntoEventType) {
        this.optionalInsertIntoEventType = optionalInsertIntoEventType;
    }

    public boolean isFireAndForget() {
        return isFireAndForget;
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return compileTimeServices.getEventTypeAvroHandler();
    }

    public EventTypeCompileTimeResolver getEventTypeCompileTimeResolver() {
        return compileTimeServices.getEventTypeCompileTimeResolver();
    }

    public GroupByRollupInfo getGroupByRollupInfo() {
        return groupByRollupInfo;
    }

    public SerdeCompileTimeResolver getSerdeResolver() {
        return compileTimeServices.getSerdeResolver();
    }

    public String getModuleName() {
        return statementRawInfo.getModuleName();
    }
}
