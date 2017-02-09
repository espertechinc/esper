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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EvalNodeUtil {
    private static final Logger log = LoggerFactory.getLogger(EvalNodeUtil.class);

    /**
     * Searched recursivly for pattern evaluation filter nodes.
     *
     * @param currentNode is the root node
     * @return list of filter nodes
     */
    public static EvalNodeAnalysisResult recursiveAnalyzeChildNodes(EvalFactoryNode currentNode) {
        EvalNodeAnalysisResult evalNodeAnalysisResult = new EvalNodeAnalysisResult();
        recursiveAnalyzeChildNodes(evalNodeAnalysisResult, currentNode);
        return evalNodeAnalysisResult;
    }

    private static void recursiveAnalyzeChildNodes(EvalNodeAnalysisResult evalNodeAnalysisResult, EvalFactoryNode currentNode) {
        if ((currentNode instanceof EvalFilterFactoryNode) ||
                (currentNode instanceof EvalGuardFactoryNode) ||
                (currentNode instanceof EvalObserverFactoryNode) ||
                (currentNode instanceof EvalMatchUntilFactoryNode) ||
                (currentNode instanceof EvalEveryDistinctFactoryNode)) {
            evalNodeAnalysisResult.addNode(currentNode);
        }

        for (EvalFactoryNode node : currentNode.getChildNodes()) {
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
    public static Set<EvalFactoryNode> recursiveGetChildNodes(EvalFactoryNode currentNode, EvalNodeUtilFactoryFilter filter) {
        Set<EvalFactoryNode> result = new LinkedHashSet<EvalFactoryNode>();
        if (filter.consider(currentNode)) {
            result.add(currentNode);
        }
        recursiveGetChildNodes(result, currentNode, filter);
        return result;
    }

    private static void recursiveGetChildNodes(Set<EvalFactoryNode> set, EvalFactoryNode currentNode, EvalNodeUtilFactoryFilter filter) {
        for (EvalFactoryNode node : currentNode.getChildNodes()) {
            if (filter.consider(node)) {
                set.add(node);
            }
            recursiveGetChildNodes(set, node, filter);
        }
    }

    public static EvalRootNode makeRootNodeFromFactory(EvalRootFactoryNode rootFactoryNode, PatternAgentInstanceContext patternAgentInstanceContext) {
        return (EvalRootNode) rootFactoryNode.makeEvalNode(patternAgentInstanceContext, null);
    }

    public static EvalNode makeEvalNodeSingleChild(List<EvalFactoryNode> childNodes, PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        return childNodes.get(0).makeEvalNode(agentInstanceContext, parentNode);
    }

    public static EvalNode[] makeEvalNodeChildren(List<EvalFactoryNode> childNodes, PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode[] children = new EvalNode[childNodes.size()];
        for (int i = 0; i < childNodes.size(); i++) {
            children[i] = childNodes.get(i).makeEvalNode(agentInstanceContext, parentNode);
        }
        return children;
    }
}
