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
package com.espertech.esper.core.context.mgr;

public class ContextControllerStatementDesc {

    private final ContextControllerStatementBase statement;
    private final ContextControllerStatementCtxCache[] caches;

    public ContextControllerStatementDesc(ContextControllerStatementBase statement, ContextControllerStatementCtxCache[] caches) {
        this.statement = statement;
        this.caches = caches;
    }

    public ContextControllerStatementBase getStatement() {
        return statement;
    }

    public ContextControllerStatementCtxCache[] getCaches() {
        return caches;
    }
}
