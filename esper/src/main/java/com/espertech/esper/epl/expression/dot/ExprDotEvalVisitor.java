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
package com.espertech.esper.epl.expression.dot;

public interface ExprDotEvalVisitor {
    public void visitPropertySource();

    public void visitEnumeration(String name);

    public void visitMethod(String methodName);

    public void visitDateTime();

    public void visitUnderlyingEvent();

    public void visitUnderlyingEventColl();

    public void visitArraySingleItemSource();

    public void visitArrayLength();
}
