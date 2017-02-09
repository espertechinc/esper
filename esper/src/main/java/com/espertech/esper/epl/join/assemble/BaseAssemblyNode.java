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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.join.rep.Node;
import com.espertech.esper.util.IndentWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a node in a tree responsible for assembling outer join query results.
 * <p>
 * The tree is double-linked, child nodes know each parent and parent know all child nodes.
 * <p>
 * Each specific subclass of this abstract assembly node is dedicated to assembling results for
 * a certain event stream.
 */
public abstract class BaseAssemblyNode implements ResultAssembler {
    /**
     * Parent node.
     */
    protected ResultAssembler parentNode;

    /**
     * Child nodes.
     */
    protected final List<BaseAssemblyNode> childNodes;

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
    protected BaseAssemblyNode(int streamNum, int numStreams) {
        this.streamNum = streamNum;
        this.numStreams = numStreams;
        childNodes = new ArrayList<BaseAssemblyNode>(4);
    }

    /**
     * Provides results to assembly nodes for initialization.
     *
     * @param result is a list of result nodes per stream
     */
    public abstract void init(List<Node>[] result);

    /**
     * Process results.
     *
     * @param result          is a list of result nodes per stream
     * @param resultFinalRows final row collection
     * @param resultRootEvent root event
     */
    public abstract void process(List<Node>[] result, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent);

    /**
     * Output this node using writer, not outputting child nodes.
     *
     * @param indentWriter to use for output
     */
    public abstract void print(IndentWriter indentWriter);

    /**
     * Set parent node.
     *
     * @param resultAssembler is the parent node
     */
    public void setParentAssembler(ResultAssembler resultAssembler) {
        this.parentNode = resultAssembler;
    }

    /**
     * Add a child node.
     *
     * @param childNode to add
     */
    public void addChild(BaseAssemblyNode childNode) {
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
    protected List<BaseAssemblyNode> getChildNodes() {
        return childNodes;
    }

    /**
     * Returns an array of stream numbers that lists all child node's stream numbers.
     *
     * @return child node stream numbers
     */
    protected int[] getSubstreams() {
        List<Integer> substreams = new LinkedList<Integer>();
        recusiveAddSubstreams(substreams);

        // copy to array
        int[] substreamArr = new int[substreams.size()];
        int count = 0;
        for (Integer stream : substreams) {
            substreamArr[count++] = stream;
        }

        return substreamArr;
    }

    private void recusiveAddSubstreams(List<Integer> substreams) {
        substreams.add(streamNum);
        for (BaseAssemblyNode child : childNodes) {
            child.recusiveAddSubstreams(substreams);
        }
    }
}
