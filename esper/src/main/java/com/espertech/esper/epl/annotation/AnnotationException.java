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
package com.espertech.esper.epl.annotation;

/**
 * Thrown to indicate a problem processing an EPL statement annotation.
 */
public class AnnotationException extends Exception {
    private static final long serialVersionUID = 167248816444780182L;

    /**
     * Ctor.
     *
     * @param message error message
     */
    public AnnotationException(String message) {
        super(message);
    }
}
