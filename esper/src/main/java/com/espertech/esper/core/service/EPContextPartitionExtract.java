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
package com.espertech.esper.core.service;

import com.espertech.esper.client.context.ContextPartitionCollection;

import java.io.Serializable;

public class EPContextPartitionExtract implements Serializable {
    private static final long serialVersionUID = 5200820792354147769L;
    private final ContextPartitionCollection collection;
    private final EPContextPartitionImportable importable;
    private final int numNestingLevels;

    public EPContextPartitionExtract(ContextPartitionCollection collection, EPContextPartitionImportable importable, int numNestingLevels) {
        this.collection = collection;
        this.importable = importable;
        this.numNestingLevels = numNestingLevels;
    }

    public ContextPartitionCollection getCollection() {
        return collection;
    }

    public EPContextPartitionImportable getImportable() {
        return importable;
    }

    public int getNumNestingLevels() {
        return numNestingLevels;
    }
}
