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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.util.TypeWidener;

import java.lang.annotation.Annotation;

public class InternalEventRouterDesc {
    private TypeWidener[] wideners;
    private EventType eventType;
    private Annotation[] annotations;
    private ExprEvaluator optionalWhereClauseEval;
    private String[] properties;
    private ExprEvaluator[] assignments;

    public void setWideners(TypeWidener[] wideners) {
        this.wideners = wideners;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public void setOptionalWhereClauseEval(ExprEvaluator optionalWhereClauseEval) {
        this.optionalWhereClauseEval = optionalWhereClauseEval;
    }

    public void setProperties(String[] properties) {
        this.properties = properties;
    }

    public void setAssignments(ExprEvaluator[] assignments) {
        this.assignments = assignments;
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

    public String[] getProperties() {
        return properties;
    }

    public ExprEvaluator[] getAssignments() {
        return assignments;
    }
}
