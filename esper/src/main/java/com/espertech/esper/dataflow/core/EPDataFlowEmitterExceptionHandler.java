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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.dataflow.EPDataFlowExceptionContext;
import com.espertech.esper.client.dataflow.EPDataFlowExceptionHandler;
import com.espertech.esper.util.AuditPath;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class EPDataFlowEmitterExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EPDataFlowEmitterExceptionHandler.class);

    private final String engineURI;
    private final String statementName;
    private final boolean audit;
    private final String dataFlowName;
    private final String operatorName;
    private final int operatorNumber;
    private final String operatorPrettyPrint;
    private final EPDataFlowExceptionHandler optionalExceptionHandler;

    public EPDataFlowEmitterExceptionHandler(String engineURI, String statementName, boolean audit, String dataFlowName, String operatorName, int operatorNumber, String operatorPrettyPrint, EPDataFlowExceptionHandler optionalExceptionHandler) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.audit = audit;
        this.dataFlowName = dataFlowName;
        this.operatorName = operatorName;
        this.operatorNumber = operatorNumber;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.optionalExceptionHandler = optionalExceptionHandler;
    }

    public void handleException(Object targetObject, FastMethod fastMethod, InvocationTargetException ex, Object[] parameters) {
        log.error("Exception encountered: " + ex.getTargetException().getMessage(), ex.getTargetException());

        if (optionalExceptionHandler != null) {
            optionalExceptionHandler.handle(new EPDataFlowExceptionContext(dataFlowName, operatorName, operatorNumber, operatorPrettyPrint, ex.getTargetException()));
        }
    }

    public String getEngineURI() {
        return engineURI;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getDataFlowName() {
        return dataFlowName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public int getOperatorNumber() {
        return operatorNumber;
    }

    public String getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }

    public EPDataFlowExceptionHandler getOptionalExceptionHandler() {
        return optionalExceptionHandler;
    }

    public void handleAudit(Object targetObject, Object[] parameters) {
        if (audit) {
            AuditPath.auditLog(engineURI, statementName, AuditEnum.DATAFLOW_OP, "dataflow " + dataFlowName + " operator " + operatorName + "(" + operatorNumber + ") parameters " + Arrays.toString(parameters));
        }
    }
}
