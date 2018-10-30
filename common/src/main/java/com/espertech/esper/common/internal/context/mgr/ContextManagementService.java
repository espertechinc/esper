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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.client.context.ContextStateListener;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ContextManagementService {
    void addContext(ContextDefinition contextDefinition, EPStatementInitServices services);

    void addStatement(String deploymentIdCreateContext, String contextName, ContextControllerStatementDesc statement, boolean recovery);

    void stoppedStatement(String deploymentIdCreateContext, String contextName, ContextControllerStatementDesc statement);

    ContextManager getContextManager(String deploymentIdCreateContext, String contextName);

    int getContextCount();

    void destroyedContext(String runtimeURI, String deploymentId, String contextName);

    CopyOnWriteArrayList<ContextStateListener> getListeners();

    Map<String, ContextDeployment> getDeployments();
}
