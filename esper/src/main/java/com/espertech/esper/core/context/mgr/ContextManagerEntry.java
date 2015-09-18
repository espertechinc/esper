/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.context.mgr;

import java.util.HashSet;
import java.util.Set;

public class ContextManagerEntry {
    private final ContextManager contextManager;
    private final Set<String> referringStatements;

    public ContextManagerEntry(ContextManager contextManager) {
        this.contextManager = contextManager;
        this.referringStatements = new HashSet<String>();
    }

    public ContextManager getContextManager() {
        return contextManager;
    }

    public void addStatement(String statementId) {
        referringStatements.add(statementId);
    }

    public int getStatementCount() {
        return referringStatements.size();
    }

    public void removeStatement(String statementId) {
        referringStatements.remove(statementId);
    }
}
