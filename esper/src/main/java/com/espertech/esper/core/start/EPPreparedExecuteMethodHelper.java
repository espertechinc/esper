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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.mgr.ContextManager;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.spec.StreamSpecCompiled;
import com.espertech.esper.epl.spec.TableQueryStreamSpec;

import java.util.Collection;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPPreparedExecuteMethodHelper {
    protected static void validateFAFQuery(StatementSpecCompiled statementSpec) throws ExprValidationException {
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if (!(streamSpec instanceof NamedWindowConsumerStreamSpec || streamSpec instanceof TableQueryStreamSpec)) {
                throw new ExprValidationException("On-demand queries require tables or named windows and do not allow event streams or patterns");
            }
            if (streamSpec.getViewSpecs().length != 0) {
                throw new ExprValidationException("Views are not a supported feature of on-demand queries");
            }
        }
        if (statementSpec.getOutputLimitSpec() != null) {
            throw new ExprValidationException("Output rate limiting is not a supported feature of on-demand queries");
        }
    }

    public static Collection<Integer> getAgentInstanceIds(FireAndForgetProcessor processor, ContextPartitionSelector selector, ContextManagementService contextManagementService, String contextName) {
        Collection<Integer> agentInstanceIds;
        if (selector == null || selector instanceof ContextPartitionSelectorAll) {
            agentInstanceIds = processor.getProcessorInstancesAll();
        } else {
            ContextManager contextManager = contextManagementService.getContextManager(contextName);
            if (contextManager == null) {
                throw new EPException("Context by name '" + contextName + "' could not be found");
            }
            agentInstanceIds = contextManager.getAgentInstanceIds(selector);
        }
        return agentInstanceIds;
    }
}
