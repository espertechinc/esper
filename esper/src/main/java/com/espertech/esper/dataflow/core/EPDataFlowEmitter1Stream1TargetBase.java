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

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.dataflow.util.DataFlowSignalManager;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

public abstract class EPDataFlowEmitter1Stream1TargetBase implements EPDataFlowEmitter, SubmitHandler {

    protected final int operatorNum;
    protected final DataFlowSignalManager signalManager;
    protected final SignalHandler signalHandler;
    protected final EPDataFlowEmitterExceptionHandler exceptionHandler;

    protected final FastMethod fastMethod;
    protected final Object targetObject;

    public EPDataFlowEmitter1Stream1TargetBase(int operatorNum, DataFlowSignalManager signalManager, SignalHandler signalHandler, EPDataFlowEmitterExceptionHandler exceptionHandler, ObjectBindingPair target, EngineImportService engineImportService) {
        this.operatorNum = operatorNum;
        this.signalManager = signalManager;
        this.signalHandler = signalHandler;
        this.exceptionHandler = exceptionHandler;

        FastClass fastClass = FastClass.create(engineImportService.getFastClassClassLoader(target.getTarget().getClass()), target.getTarget().getClass());
        fastMethod = fastClass.getMethod(target.getBinding().getConsumingBindingDesc().getMethod());
        targetObject = target.getTarget();
    }

    public abstract void submitInternal(Object object);

    public void submit(Object object) {
        submitInternal(object);
    }

    public void submitSignal(EPDataFlowSignal signal) {
        signalManager.processSignal(operatorNum, signal);
        signalHandler.handleSignal(signal);
    }

    public void handleSignal(EPDataFlowSignal signal) {
        signalHandler.handleSignal(signal);
    }

    public void submitPort(int portNumber, Object object) {
        if (portNumber == 0) {
            submit(object);
        }
    }

    public FastMethod getFastMethod() {
        return fastMethod;
    }
}
