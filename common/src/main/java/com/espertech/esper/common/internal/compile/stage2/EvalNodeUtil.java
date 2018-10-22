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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctForgeNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardForgeNode;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilForgeNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverForgeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class EvalNodeUtil {
    private static final Logger log = LoggerFactory.getLogger(EvalNodeUtil.class);

    public final static EvalFactoryNode[] EMPTY_FACTORY_ARRAY = new EvalFactoryNode[0];

    /**
     * Searched recursivly for pattern evaluation filter nodes.
     *
     * @param currentNode is the root node
     * @return list of filter nodes
     */
    public static EvalNodeAnalysisResult recursiveAnalyzeChildNodes(EvalForgeNode currentNode) {
        EvalNodeAnalysisResult evalNodeAnalysisResult = new EvalNodeAnalysisResult();
        recursiveAnalyzeChildNodes(evalNodeAnalysisResult, currentNode);
        return evalNodeAnalysisResult;
    }

    private static void recursiveAnalyzeChildNodes(EvalNodeAnalysisResult evalNodeAnalysisResult, EvalForgeNode currentNode) {
        if ((currentNode instanceof EvalFilterForgeNode) ||
                (currentNode instanceof EvalGuardForgeNode) ||
                (currentNode instanceof EvalObserverForgeNode) ||
                (currentNode instanceof EvalMatchUntilForgeNode) ||
                (currentNode instanceof EvalEveryDistinctForgeNode)) {
            evalNodeAnalysisResult.addNode(currentNode);
        }

        if (currentNode instanceof EvalObserverForgeNode) {
            evalNodeAnalysisResult.addNode(currentNode);
        }

        for (EvalForgeNode node : currentNode.getChildNodes()) {
            recursiveAnalyzeChildNodes(evalNodeAnalysisResult, node);
        }
    }

    /**
     * Returns all child nodes as a set.
     *
     * @param currentNode parent node
     * @param filter      filter
     * @return all child nodes
     */
    public static Set<EvalForgeNode> recursiveGetChildNodes(EvalForgeNode currentNode, EvalNodeUtilFactoryFilter filter) {
        Set<EvalForgeNode> result = new LinkedHashSet<EvalForgeNode>();
        if (filter.consider(currentNode)) {
            result.add(currentNode);
        }
        recursiveGetChildNodes(result, currentNode, filter);
        return result;
    }

    private static void recursiveGetChildNodes(Set<EvalForgeNode> set, EvalForgeNode currentNode, EvalNodeUtilFactoryFilter filter) {
        for (EvalForgeNode node : currentNode.getChildNodes()) {
            if (filter.consider(node)) {
                set.add(node);
            }
            recursiveGetChildNodes(set, node, filter);
        }
    }

    public static EvalNode makeEvalNodeSingleChild(EvalFactoryNode child, PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        return child.makeEvalNode(agentInstanceContext, parentNode);
    }

    public static EvalRootNode makeRootNodeFromFactory(EvalRootFactoryNode rootFactoryNode, PatternAgentInstanceContext patternAgentInstanceContext) {
        return (EvalRootNode) rootFactoryNode.makeEvalNode(patternAgentInstanceContext, null);
    }

    public static EvalNode[] makeEvalNodeChildren(EvalFactoryNode[] factories, PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] children = new EvalNode[factories.length];
        for (int i = 0; i < factories.length; i++) {
            children[i] = factories[i].makeEvalNode(agentInstanceContext, parentNode);
        }
        return children;
    }
}
