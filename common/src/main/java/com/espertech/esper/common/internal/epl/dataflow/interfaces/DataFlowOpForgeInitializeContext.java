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
package com.espertech.esper.common.internal.epl.dataflow.interfaces;

import com.espertech.esper.common.internal.compile.stage1.spec.GraphOperatorSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;

import java.lang.annotation.Annotation;
import java.util.Map;

public class DataFlowOpForgeInitializeContext {

    private final String dataflowName;
    private final int operatorNumber;
    private final Annotation[] operatorAnnotations;
    private final GraphOperatorSpec operatorSpec;
    private final Map<Integer, DataFlowOpInputPort> inputPorts;
    private final Map<Integer, DataFlowOpOutputPort> outputPorts;
    private final DataFlowOpForgeCodegenEnv codegenEnv;
    private final StatementBaseInfo base;
    private final StatementCompileTimeServices services;

    public DataFlowOpForgeInitializeContext(String dataflowName, int operatorNumber, Annotation[] operatorAnnotations, GraphOperatorSpec operatorSpec, Map<Integer, DataFlowOpInputPort> inputPorts, Map<Integer, DataFlowOpOutputPort> outputPorts, DataFlowOpForgeCodegenEnv codegenEnv, StatementBaseInfo base, StatementCompileTimeServices services) {
        this.dataflowName = dataflowName;
        this.operatorNumber = operatorNumber;
        this.operatorAnnotations = operatorAnnotations;
        this.operatorSpec = operatorSpec;
        this.inputPorts = inputPorts;
        this.outputPorts = outputPorts;
        this.codegenEnv = codegenEnv;
        this.base = base;
        this.services = services;
    }

    public String getDataflowName() {
        return dataflowName;
    }

    public Annotation[] getOperatorAnnotations() {
        return operatorAnnotations;
    }

    public GraphOperatorSpec getOperatorSpec() {
        return operatorSpec;
    }

    public StatementBaseInfo getBase() {
        return base;
    }

    public StatementCompileTimeServices getServices() {
        return services;
    }

    public Map<Integer, DataFlowOpInputPort> getInputPorts() {
        return inputPorts;
    }

    public Map<Integer, DataFlowOpOutputPort> getOutputPorts() {
        return outputPorts;
    }

    public StatementRawInfo getStatementRawInfo() {
        return base.getStatementRawInfo();
    }

    public int getOperatorNumber() {
        return operatorNumber;
    }

    public DataFlowOpForgeCodegenEnv getCodegenEnv() {
        return codegenEnv;
    }
}
