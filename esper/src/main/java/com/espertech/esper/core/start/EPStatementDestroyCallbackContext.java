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
package com.espertech.esper.core.start;

import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.util.DestroyCallback;

public class EPStatementDestroyCallbackContext implements DestroyCallback {
    private final ContextManagementService contextManagementService;
    private final String contextName;
    private final String statementName;
    private final int statementId;

    public EPStatementDestroyCallbackContext(ContextManagementService contextManagementService, String optionalContextName, String statementName, int statementId) {
        this.contextManagementService = contextManagementService;
        this.contextName = optionalContextName;
        this.statementName = statementName;
        this.statementId = statementId;
    }

    public void destroy() {
        contextManagementService.destroyedStatement(contextName, statementName, statementId);
    }
}