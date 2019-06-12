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
package com.espertech.esper.common.internal.compile.stage2;

public class StatementSpecCompileSyntaxException extends StatementSpecCompileException {
    private static final long serialVersionUID = -6026077468940980076L;

    public StatementSpecCompileSyntaxException(String message, String expression) {
        super(message, expression);
    }

    public StatementSpecCompileSyntaxException(String message, Throwable cause, String expression) {
        super(message, cause, expression);
    }
}
