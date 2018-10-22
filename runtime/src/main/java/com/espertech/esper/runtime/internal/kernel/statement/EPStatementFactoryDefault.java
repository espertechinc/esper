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
package com.espertech.esper.runtime.internal.kernel.statement;

public class EPStatementFactoryDefault implements EPStatementFactory {
    public final static EPStatementFactoryDefault INSTANCE = new EPStatementFactoryDefault();

    private EPStatementFactoryDefault() {
    }

    public EPStatementSPI statement(EPStatementFactoryArgs args) {
        return new EPStatementImpl(args);
    }
}
