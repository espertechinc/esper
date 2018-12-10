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

import java.util.Map;

public class CompilableItemResult {
    private final Map<String, byte[]> statementBytes;
    private final Throwable exception;

    public CompilableItemResult(Map<String, byte[]> statementBytes) {
        this.statementBytes = statementBytes;
        this.exception = null;
    }

    public CompilableItemResult(Throwable exception) {
        this.exception = exception;
        this.statementBytes = null;
    }

    public Map<String, byte[]> getStatementBytes() {
        return statementBytes;
    }

    public Throwable getException() {
        return exception;
    }
}
