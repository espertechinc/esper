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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.spec.UpdateDesc;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.util.TypeWidener;

import java.lang.annotation.Annotation;

public class InternalEventRouterDesc {
    private final UpdateDesc updateDesc;
    private final EventBeanCopyMethod copyMethod;
    private final TypeWidener[] wideners;
    private final EventType eventType;
    private final Annotation[] annotations;
    private final ExprEvaluator optionalWhereClauseEval;

    public InternalEventRouterDesc(UpdateDesc updateDesc, EventBeanCopyMethod copyMethod, TypeWidener[] wideners, EventType eventType, Annotation[] annotations, EngineImportService engineImportService, String statementName) {
        this.updateDesc = updateDesc;
        this.copyMethod = copyMethod;
        this.wideners = wideners;
        this.eventType = eventType;
        this.annotations = annotations;
        optionalWhereClauseEval = updateDesc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(updateDesc.getOptionalWhereClause().getForge(), engineImportService, this.getClass(), false, statementName);
    }

    public UpdateDesc getUpdateDesc() {
        return updateDesc;
    }

    public EventBeanCopyMethod getCopyMethod() {
        return copyMethod;
    }

    public TypeWidener[] getWideners() {
        return wideners;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public ExprEvaluator getOptionalWhereClauseEval() {
        return optionalWhereClauseEval;
    }
}
