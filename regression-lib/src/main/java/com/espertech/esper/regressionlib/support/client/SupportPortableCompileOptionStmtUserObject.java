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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.compiler.client.option.StatementUserObjectContext;
import com.espertech.esper.compiler.client.option.StatementUserObjectOption;

import java.io.Serializable;

public class SupportPortableCompileOptionStmtUserObject implements StatementUserObjectOption, Serializable {
    private static final long serialVersionUID = -1185668934344418515L;
    private final Serializable value;

    public SupportPortableCompileOptionStmtUserObject(Serializable value) {
        this.value = value;
    }

    public Serializable getValue(StatementUserObjectContext env) {
        return value;
    }

    public Serializable getValue() {
        return value;
    }
}
