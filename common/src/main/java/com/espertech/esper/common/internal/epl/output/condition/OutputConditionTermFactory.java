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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

public class OutputConditionTermFactory implements OutputConditionFactory {

    public final static OutputConditionTermFactory INSTANCE = new OutputConditionTermFactory();

    private OutputConditionTermFactory() {
    }

    public OutputCondition instantiateOutputCondition(AgentInstanceContext agentInstanceContext, OutputCallback outputCallback) {
        return new OutputConditionTerm(outputCallback);
    }
}
