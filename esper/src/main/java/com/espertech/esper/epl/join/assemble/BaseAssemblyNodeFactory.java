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
package com.espertech.esper.epl.join.assemble;

import com.espertech.esper.util.IndentWriter;

import java.util.*;

/**
 * Represents the factory of a node in a tree responsible for assembling outer join query results.
 * <p>
 * The tree of factory nodes is double-linked, child nodes know each parent and parent know all child nodes.
 */
public abstract class BaseAssemblyNodeFactory {
    /**
     * Parent node.
     */
    protected BaseAssemblyNodeFactory parentNode;

    /**
     * Child nodes.
     */
    protected final List<BaseAssemblyNodeFactory> childNodes;

    /**
     * Stream number.
     */
    protected final int streamNum;

    /**
     * Number of streams in statement.
     */
    protected final int numStreams;

    /**
     * Ctor.
     *
     * @param streamNum  - stream number of the event stream that this node assembles results for.
     * @param numStreams - number of streams
     */
    protected BaseAssemblyNodeFactory(int streamNum, int numStreams) {
        this.streamNum = streamNum;
        this.numStreams = numStreams;
        childNodes = new ArrayList<BaseAssemblyNodeFactory>(4);
    }

    public abstract BaseAssemblyNode makeAssemblerUnassociated();

    /**
     * Output this node using writer, not outputting child nodes.
     *
     * @param indentWriter to use for output
     */
    public abstract void print(IndentWriter indentWriter);

    /**
     * Set parent node.
     *
     * @param parent parent node
     */
    public void setParent(BaseAssemblyNodeFactory parent) {
        this.parentNode = parent;
    }

    public BaseAssemblyNodeFactory getParentNode() {
        return parentNode;
    }

    /**
     * Add a child node.
     *
     * @param childNode to add
     */
    public void addChild(BaseAssemblyNodeFactory childNode) {
        childNode.parentNode = this;
        childNodes.add(childNode);
    }

    /**
     * Returns the stream number.
     *
     * @return stream number
     */
    protected int getStreamNum() {
        return streamNum;
    }

    /**
     * Returns child nodes.
     *
     * @return child nodes
     */
    public List<BaseAssemblyNodeFactory> getChildNodes() {
        return childNodes;
    }

    /**
     * Output this node and all descendent nodes using writer, outputting child nodes.
     *
     * @param indentWriter to output to
     */
    public void printDescendends(IndentWriter indentWriter) {
        this.print(indentWriter);
        for (BaseAssemblyNodeFactory child : childNodes) {
            indentWriter.incrIndent();
            child.print(indentWriter);
            indentWriter.decrIndent();
        }
    }

    /**
     * Returns all descendent nodes to the top node in a list in which the utmost descendants are
     * listed first and the top node itself is listed last.
     *
     * @param topNode is the root node of a tree structure
     * @return list of nodes with utmost descendants first ordered by level of depth in tree with top node last
     */
    public static List<BaseAssemblyNodeFactory> getDescendentNodesBottomUp(BaseAssemblyNodeFactory topNode) {
        List<BaseAssemblyNodeFactory> result = new LinkedList<BaseAssemblyNodeFactory>();

        // Map to hold per level of the node (1 to N depth) of node a list of nodes, if any
        // exist at that level
        TreeMap<Integer, List<BaseAssemblyNodeFactory>> nodesPerLevel = new TreeMap<Integer, List<BaseAssemblyNodeFactory>>();

        // Recursively enter all aggregate functions and their level into map
        recursiveAggregateEnter(topNode, nodesPerLevel, 1);

        // Done if none found
        if (nodesPerLevel.isEmpty()) {
            throw new IllegalStateException("Empty collection for nodes per level");
        }

        // From the deepest (highest) level to the lowest, add aggregates to list
        int deepLevel = nodesPerLevel.lastKey();
        for (int i = deepLevel; i >= 1; i--) {
            List<BaseAssemblyNodeFactory> list = nodesPerLevel.get(i);
            if (list == null) {
                continue;
            }
            result.addAll(list);
        }

        return result;
    }

    private static void recursiveAggregateEnter(BaseAssemblyNodeFactory currentNode, Map<Integer, List<BaseAssemblyNodeFactory>> nodesPerLevel, int currentLevel) {
        // ask all child nodes to enter themselves
        for (BaseAssemblyNodeFactory node : currentNode.childNodes) {
            recursiveAggregateEnter(node, nodesPerLevel, currentLevel + 1);
        }

        // Add myself to list
        List<BaseAssemblyNodeFactory> aggregates = nodesPerLevel.get(currentLevel);
        if (aggregates == null) {
            aggregates = new LinkedList<BaseAssemblyNodeFactory>();
            nodesPerLevel.put(currentLevel, aggregates);
        }
        aggregates.add(currentNode);
    }
}
