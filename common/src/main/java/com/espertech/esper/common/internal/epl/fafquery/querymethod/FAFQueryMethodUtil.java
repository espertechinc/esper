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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextManager;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;

import java.util.Collection;

public class FAFQueryMethodUtil {

    static Collection<Integer> agentInstanceIds(FireAndForgetProcessor processor, ContextPartitionSelector optionalSelector, ContextManagementService contextManagementService) {
        ContextManager contextManager = contextManagementService.getContextManager(processor.getContextDeploymentId(), processor.getContextName());
        return contextManager.getRealization().getAgentInstanceIds(optionalSelector == null ? ContextPartitionSelectorAll.INSTANCE : optionalSelector);
    }

    public static EPException runtimeDestroyed() {
        return new EPException("Runtime has already been destroyed");
    }
}
