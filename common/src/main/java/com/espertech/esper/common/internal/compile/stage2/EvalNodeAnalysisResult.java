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

import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilForgeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of analysis of pattern expression node tree.
 */
public class EvalNodeAnalysisResult {
    private List<EvalForgeNode> activeNodes = new ArrayList<EvalForgeNode>();

    /**
     * Add a node found.
     *
     * @param node found
     */
    public void addNode(EvalForgeNode node) {
        activeNodes.add(node);
    }

    /**
     * Returns all nodes found.
     *
     * @return pattern nodes
     */
    public List<EvalForgeNode> getActiveNodes() {
        return activeNodes;
    }

    public List<EvalFilterForgeNode> getFilterNodes() {
        List<EvalFilterForgeNode> filterNodes = new ArrayList<>();
        for (EvalForgeNode node : activeNodes) {
            if (node instanceof EvalFilterForgeNode) {
                filterNodes.add((EvalFilterForgeNode) node);
            }
        }
        return filterNodes;
    }

    public List<EvalMatchUntilForgeNode> getRepeatNodes() {
        List<EvalMatchUntilForgeNode> filterNodes = new ArrayList<>();
        for (EvalForgeNode node : activeNodes) {
            if (node instanceof EvalMatchUntilForgeNode) {
                filterNodes.add((EvalMatchUntilForgeNode) node);
            }
        }
        return filterNodes;
    }
}
