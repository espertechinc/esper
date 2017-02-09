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
 * Assembly node factory for an event stream that is a branch with a single required child node below it.
 */
public class BranchRequiredAssemblyNodeFactory extends BaseAssemblyNodeFactory {
    /**
     * Ctor.
     *
     * @param streamNum  - is the stream number
     * @param numStreams - is the number of streams
     */
    public BranchRequiredAssemblyNodeFactory(int streamNum, int numStreams) {
        super(streamNum, numStreams);
    }

    public void print(IndentWriter indentWriter) {
        indentWriter.println("BranchRequiredAssemblyNode streamNum=" + streamNum);
    }

    public BaseAssemblyNode makeAssemblerUnassociated() {
        return new BranchRequiredAssemblyNode(streamNum, numStreams);
    }
}
