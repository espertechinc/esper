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
package com.espertech.esper.pattern;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.filterspec.MatchedEventMapMeta;

import java.util.IdentityHashMap;

/**
 * Factory for pattern context instances, creating context objects for each distinct pattern based on the
 * patterns root node and stream id.
 */
public interface PatternContextFactory {
    /**
     * Create a pattern context.
     *
     * @param statementContext    is the statement information and services
     * @param streamId            is the stream id
     * @param rootNode            is the pattern root node
     * @param allowResilient      indicator whether resilience is allowed or not
     * @param matchedEventMapMeta match metadata
     * @return pattern context
     */
    public PatternContext createContext(StatementContext statementContext,
                                        int streamId,
                                        EvalRootFactoryNode rootNode,
                                        MatchedEventMapMeta matchedEventMapMeta,
                                        boolean allowResilient);

    public PatternAgentInstanceContext createPatternAgentContext(PatternContext patternContext,
                                                                 AgentInstanceContext agentInstanceContext,
                                                                 boolean hasConsumingFilter,
                                                                 IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum);
}
