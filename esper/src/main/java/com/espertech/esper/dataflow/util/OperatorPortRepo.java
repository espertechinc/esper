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
package com.espertech.esper.dataflow.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class OperatorPortRepo {

    private final List<Method> inputPorts;
    private final List<Method> outputPorts;

    public OperatorPortRepo() {
        inputPorts = new ArrayList<Method>();
        outputPorts = new ArrayList<Method>();
    }

    public List<Method> getInputPorts() {
        return inputPorts;
    }

    public List<Method> getOutputPorts() {
        return outputPorts;
    }

    public String toString() {
        return "OperatorPorts{" +
                "inputPorts=" + inputPorts +
                ", outputPorts=" + outputPorts +
                '}';
    }
}
