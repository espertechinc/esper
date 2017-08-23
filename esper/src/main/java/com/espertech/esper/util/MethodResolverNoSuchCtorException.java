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

import java.lang.reflect.Constructor;

/**
 * Exception for resolution of a method failed.
 */
public class MethodResolverNoSuchCtorException extends Exception {
    private static final long serialVersionUID = 5903661121726479172L;

    private transient Constructor nearestMissCtor;

    /**
     * Ctor.
     *
     * @param message         message
     * @param nearestMissCtor best-match method
     */
    public MethodResolverNoSuchCtorException(String message, Constructor nearestMissCtor) {
        super(message);
        this.nearestMissCtor = nearestMissCtor;
    }

    /**
     * Returns the best-match ctor.
     *
     * @return ctor
     */
    public Constructor getNearestMissCtor() {
        return nearestMissCtor;
    }
}
