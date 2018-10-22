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
package com.espertech.esper.common.internal.event.property;

/**
 * Encapsulates the parse result parsing a mapped property as a class and method name with args.
 */
public class MappedPropertyParseResult {
    private String className;
    private String methodName;
    private String argString;

    /**
     * Returns class name.
     *
     * @return name of class
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the method name.
     *
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the method argument.
     *
     * @return arg
     */
    public String getArgString() {
        return argString;
    }

    /**
     * Returns the parse result of the mapped property.
     *
     * @param className  is the class name, or null if there isn't one
     * @param methodName is the method name
     * @param argString  is the argument
     */
    public MappedPropertyParseResult(String className, String methodName, String argString) {
        this.className = className;
        this.methodName = methodName;
        this.argString = argString;
    }
}
