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
package com.espertech.esper.util;

import java.lang.reflect.Method;

/**
 * Exception for resolution of a method failed.
 */
public class MethodResolverNoSuchMethodException extends Exception {
    private transient Method nearestMissMethod;
    private static final long serialVersionUID = 9217764859358996087L;

    /**
     * Ctor.
     *
     * @param message           message
     * @param nearestMissMethod best-match method
     */
    public MethodResolverNoSuchMethodException(String message, Method nearestMissMethod) {
        super(message);
        this.nearestMissMethod = nearestMissMethod;
    }

    /**
     * Returns the best-match method.
     *
     * @return method
     */
    public Method getNearestMissMethod() {
        return nearestMissMethod;
    }
}
