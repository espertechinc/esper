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
package com.espertech.esper.common.internal.epl.dataflow.realize;

import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.common.internal.epl.dataflow.util.DataFlowSignalManager;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

import java.lang.reflect.Method;

public abstract class EPDataFlowEmitter1Stream1TargetBase implements EPDataFlowEmitter, SubmitHandler {

    protected final int operatorNum;
    protected final DataFlowSignalManager signalManager;
    protected final SignalHandler signalHandler;
    protected final EPDataFlowEmitterExceptionHandler exceptionHandler;

    protected final Method fastMethod;
    protected final Object targetObject;

    public EPDataFlowEmitter1Stream1TargetBase(int operatorNum, DataFlowSignalManager signalManager, SignalHandler signalHandler, EPDataFlowEmitterExceptionHandler exceptionHandler, ObjectBindingPair target, ClasspathImportService classpathImportService) {
        this.operatorNum = operatorNum;
        this.signalManager = signalManager;
        this.signalHandler = signalHandler;
        this.exceptionHandler = exceptionHandler;

        fastMethod = target.getBinding().getConsumingBindingDesc().getMethod();
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

    public Method getFastMethod() {
        return fastMethod;
    }
}
