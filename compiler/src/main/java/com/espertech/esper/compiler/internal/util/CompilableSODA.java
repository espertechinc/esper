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

import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompilableSODA implements Compilable {
    private static final Logger log = LoggerFactory.getLogger(CompilableSODA.class);

    private final EPStatementObjectModel soda;
    private final int lineNumber;

    public CompilableSODA(EPStatementObjectModel soda, int lineNumber) {
        this.soda = soda;
        this.lineNumber = lineNumber;
    }

    public EPStatementObjectModel getSoda() {
        return soda;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String toEPL() {
        try {
            return soda.toEPL();
        } catch (Throwable t) {
            log.debug("Failed to get EPL from SODA: " + t.getMessage(), t);
            return "(cannot obtain EPL expression)";
        }
    }
}
