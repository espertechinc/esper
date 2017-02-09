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

/**
 * Assembly node for an event stream that is a branch with a two or more child nodes (required and optional) below it.
 */
public class CartesianProdAssemblyNodeFactory extends BaseAssemblyNodeFactory {
    private final int[] childStreamIndex; // maintain mapping of stream number to index in array
    private final boolean allSubStreamsOptional;

    /**
     * Ctor.
     *
     * @param streamNum             - is the stream number
     * @param numStreams            - is the number of streams
     * @param allSubStreamsOptional - true if all child nodes to this node are optional, or false if
     *                              one or more child nodes are required for a result.
     */
    public CartesianProdAssemblyNodeFactory(int streamNum, int numStreams, boolean allSubStreamsOptional) {
        super(streamNum, numStreams);
        this.childStreamIndex = new int[numStreams];
        this.allSubStreamsOptional = allSubStreamsOptional;
    }

    @Override
    public void addChild(BaseAssemblyNodeFactory childNode) {
        childStreamIndex[childNode.getStreamNum()] = childNodes.size();
        super.addChild(childNode);
    }

    public void print(IndentWriter indentWriter) {
        indentWriter.println("CartesianProdAssemblyNode streamNum=" + streamNum);
    }

    public BaseAssemblyNode makeAssemblerUnassociated() {
        return new CartesianProdAssemblyNode(streamNum, numStreams, allSubStreamsOptional, childStreamIndex);
    }
}
