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

import java.util.ArrayList;
import java.util.List;

/**
 * Result of analysis of pattern expression node tree.
 */
public class EvalNodeAnalysisResult {
    private List<EvalFactoryNode> activeNodes = new ArrayList<EvalFactoryNode>();

    /**
     * Add a node found.
     *
     * @param node found
     */
    public void addNode(EvalFactoryNode node) {
        activeNodes.add(node);
    }

    /**
     * Returns all nodes found.
     *
     * @return pattern nodes
     */
    public List<EvalFactoryNode> getActiveNodes() {
        return activeNodes;
    }

    /**
     * Returns filter nodes.
     *
     * @return filter nodes
     */
    public List<EvalFilterFactoryNode> getFilterNodes() {
        List<EvalFilterFactoryNode> filterNodes = new ArrayList<EvalFilterFactoryNode>();
        for (EvalFactoryNode node : activeNodes) {
            if (node instanceof EvalFilterFactoryNode) {
                filterNodes.add((EvalFilterFactoryNode) node);
            }
        }
        return filterNodes;
    }

    /**
     * Returns the repeat-nodes.
     *
     * @return repeat nodes
     */
    public List<EvalMatchUntilFactoryNode> getRepeatNodes() {
        List<EvalMatchUntilFactoryNode> filterNodes = new ArrayList<EvalMatchUntilFactoryNode>();
        for (EvalFactoryNode node : activeNodes) {
            if (node instanceof EvalMatchUntilFactoryNode) {
                filterNodes.add((EvalMatchUntilFactoryNode) node);
            }
        }
        return filterNodes;
    }
}
