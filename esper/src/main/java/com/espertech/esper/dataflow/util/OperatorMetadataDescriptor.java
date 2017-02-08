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

import com.espertech.esper.epl.spec.GraphOperatorSpec;

import java.lang.annotation.Annotation;

public class OperatorMetadataDescriptor {
    private final GraphOperatorSpec operatorSpec;
    private final int operatorNumber;
    private final Class operatorClass;
    private final Class operatorFactoryClass;
    private final Object optionalOperatorObject;
    private final String operatorPrettyPrint;
    private final Annotation[] operatorAnnotations;

    public OperatorMetadataDescriptor(GraphOperatorSpec operatorSpec, int operatorNumber, Class operatorClass, Class operatorFactoryClass, Object optionalOperatorObject, String operatorPrettyPrint, Annotation[] operatorAnnotations) {
        this.operatorSpec = operatorSpec;
        this.operatorNumber = operatorNumber;
        this.operatorClass = operatorClass;
        this.operatorFactoryClass = operatorFactoryClass;
        this.optionalOperatorObject = optionalOperatorObject;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.operatorAnnotations = operatorAnnotations;
    }

    public GraphOperatorSpec getOperatorSpec() {
        return operatorSpec;
    }

    public String getOperatorName() {
        return operatorSpec.getOperatorName();
    }

    public Class getOperatorClass() {
        return operatorClass;
    }

    public Class getOperatorFactoryClass() {
        return operatorFactoryClass;
    }

    public Object getOptionalOperatorObject() {
        return optionalOperatorObject;
    }

    public int getOperatorNumber() {
        return operatorNumber;
    }

    public String getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }

    public Annotation[] getOperatorAnnotations() {
        return operatorAnnotations;
    }
}
