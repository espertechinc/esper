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
package com.espertech.esper.compiler.internal.util;

public class CompilableItemResult {
    private final Throwable exception;

    public CompilableItemResult() {
        this.exception = null;
    }

    public CompilableItemResult(Throwable exception) {
        this.exception = exception;
    }

    public Throwable getException() {
        return exception;
    }
}
