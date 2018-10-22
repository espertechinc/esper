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
package com.espertech.esper.regressionlib.support.dataflow;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class SupportGenericOutputOpWPort<T> implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {
    private List<T> received = new ArrayList<T>();
    private List<Integer> receivedPorts = new ArrayList<Integer>();

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(SupportGenericOutputOpWPort.class);
    }

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {

    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        return new SupportGenericOutputOpWPort();
    }

    public synchronized void onInput(int port, T event) {
        received.add(event);
        receivedPorts.add(port);
    }

    public synchronized Pair<List<T>, List<Integer>> getAndReset() {
        List<T> resultEvents = received;
        List<Integer> resultPorts = receivedPorts;
        received = new ArrayList<T>();
        receivedPorts = new ArrayList<Integer>();
        return new Pair<List<T>, List<Integer>>(resultEvents, resultPorts);
    }
}

