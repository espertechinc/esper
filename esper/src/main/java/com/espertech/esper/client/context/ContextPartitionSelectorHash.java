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
package com.espertech.esper.client.context;

import java.util.Set;

/**
 * Selects context partitions based on hash codes, for use with hashed context.
 */
public interface ContextPartitionSelectorHash extends ContextPartitionSelector {
    /**
     * Returns a set of hashes.
     *
     * @return hashes
     */
    public Set<Integer> getHashes();
}
