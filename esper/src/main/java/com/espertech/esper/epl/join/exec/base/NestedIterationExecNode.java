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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.IndentWriter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Execution node that performs a nested iteration over all child nodes.
 * <p>
 * Each child node under this node typically represents a table lookup. The implementation
 * 'hops' from the first child to the next recursively for each row returned by a child.
 * <p>
 * It passes a 'prototype' row (prefillPath) to each new child which contains the current partial event set.
 */
public class NestedIterationExecNode extends ExecNode {
    private final LinkedList<ExecNode> childNodes;
    private final int[] nestedStreams;
    private int nestingOrderLength;

    /**
     * Ctor.
     *
     * @param nestedStreams - array of integers defining order of streams in nested join.
     */
    public NestedIterationExecNode(int[] nestedStreams) {
        this.nestedStreams = nestedStreams;
        this.childNodes = new LinkedList<ExecNode>();
    }

    /**
     * Add a child node.
     *
     * @param childNode to add
     */
    public void addChildNode(ExecNode childNode) {
        childNodes.add(childNode);
    }

    public void process(EventBean lookupEvent, EventBean[] prefillPath, Collection<EventBean[]> result, ExprEvaluatorContext exprEvaluatorContext) {
        nestingOrderLength = childNodes.size();
        recursiveNestedJoin(lookupEvent, 0, prefillPath, result, exprEvaluatorContext);
    }

    /**
     * Recursive method to run through all child nodes and, for each result set tuple returned
     * by a child node, execute the inner child of the child node until there are no inner child nodes.
     *
     * @param lookupEvent          - current event to use for lookup by child node
     * @param nestingOrderIndex    - index within the child nodes indicating what nesting level we are at
     * @param currentPath          - prototype result row to use by child nodes for generating result rows
     * @param result               - result tuple rows to be populated
     * @param exprEvaluatorContext context for expression evalauation
     */
    protected void recursiveNestedJoin(EventBean lookupEvent, int nestingOrderIndex, EventBean[] currentPath, Collection<EventBean[]> result, ExprEvaluatorContext exprEvaluatorContext) {
        List<EventBean[]> nestedResult = new LinkedList<EventBean[]>();
        ExecNode nestedExecNode = childNodes.get(nestingOrderIndex);
        nestedExecNode.process(lookupEvent, currentPath, nestedResult, exprEvaluatorContext);
        boolean isLastStream = nestingOrderIndex == nestingOrderLength - 1;

        // This is not the last nesting level so no result rows are added. Invoke next nesting level for
        // each event found.
        if (!isLastStream) {
            for (EventBean[] row : nestedResult) {
                EventBean lookup = row[nestedStreams[nestingOrderIndex]];
                recursiveNestedJoin(lookup, nestingOrderIndex + 1, row, result, exprEvaluatorContext);
            }
            return;
        }

        // Loop to add result rows
        for (EventBean[] row : nestedResult) {
            result.add(row);
        }
    }

    public void print(IndentWriter writer) {
        writer.println("NestedIterationExecNode");
        writer.incrIndent();

        for (ExecNode child : childNodes) {
            child.print(writer);
        }
        writer.decrIndent();
    }
}
