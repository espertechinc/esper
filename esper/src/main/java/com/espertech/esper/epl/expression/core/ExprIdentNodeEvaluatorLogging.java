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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.util.AuditPath;

public class ExprIdentNodeEvaluatorLogging extends ExprIdentNodeEvaluatorImpl {
    private final String engineURI;
    private final String propertyName;
    private final String statementName;

    public ExprIdentNodeEvaluatorLogging(int streamNum, EventPropertyGetter propertyGetter, Class propertyType, ExprIdentNode identNode, String propertyName, String statementName, String engineURI) {
        super(streamNum, propertyGetter, propertyType, identNode);
        this.propertyName = propertyName;
        this.statementName = statementName;
        this.engineURI = engineURI;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = super.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (AuditPath.isInfoEnabled()) {
            AuditPath.auditLog(engineURI, statementName, AuditEnum.PROPERTY, propertyName + " value " + result);
        }
        return result;
    }
}
