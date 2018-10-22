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
package com.espertech.esper.common.internal.metrics.audit;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMinimal;

public interface AuditProviderPattern {
    void patternTrue(EvalFactoryNode factoryNode, Object from, MatchedEventMapMinimal matchEvent, boolean isQuitted, AgentInstanceContext agentInstanceContext);

    void patternFalse(EvalFactoryNode factoryNode, Object from, AgentInstanceContext agentInstanceContext);
}
