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

import com.espertech.esper.dataflow.util.DataFlowSignalManager;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

import java.lang.reflect.InvocationTargetException;

public class EPDataFlowEmitter1Stream1TargetUnwind extends EPDataFlowEmitter1Stream1TargetBase {

    public EPDataFlowEmitter1Stream1TargetUnwind(int operatorNum, DataFlowSignalManager signalManager, SignalHandler signalHandler, EPDataFlowEmitterExceptionHandler exceptionHandler, ObjectBindingPair target, EngineImportService engineImportService) {
        super(operatorNum, signalManager, signalHandler, exceptionHandler, target, engineImportService);
    }

    public void submitInternal(Object object) {
        Object[] parameters = (Object[]) object;
        try {
            exceptionHandler.handleAudit(targetObject, parameters);
            fastMethod.invoke(targetObject, parameters);
        } catch (InvocationTargetException e) {
            exceptionHandler.handleException(targetObject, fastMethod, e, parameters);
        }
    }
}
