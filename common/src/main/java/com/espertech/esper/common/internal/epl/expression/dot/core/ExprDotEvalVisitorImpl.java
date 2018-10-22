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
package com.espertech.esper.common.internal.epl.expression.dot.core;

public class ExprDotEvalVisitorImpl implements ExprDotEvalVisitor {
    private String methodType;
    private String methodName;

    public void visitPropertySource() {
        set("property value", null);
    }

    public void visitEnumeration(String name) {
        set("enumeration method", name);
    }

    public void visitMethod(String methodName) {
        set("jvm method", methodName);
    }

    public void visitDateTime() {
        set("datetime method", null);
    }

    public void visitUnderlyingEvent() {
        set("underlying event", null);
    }

    public void visitUnderlyingEventColl() {
        set("underlying event collection", null);
    }

    public void visitArraySingleItemSource() {
        set("array item", null);
    }

    public void visitArrayLength() {
        set("array length", null);
    }

    public String getMethodType() {
        return methodType;
    }

    public String getMethodName() {
        return methodName;
    }

    private void set(String methodType, String methodName) {
        this.methodType = methodType;
        this.methodName = methodName;
    }
}

