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
 * Selects context partitions for use with a category context by providing a set of labels.
 */
public interface ContextPartitionSelectorCategory extends ContextPartitionSelector {
    /**
     * Returns a set of category label names.
     *
     * @return label names
     */
    public Set<String> getLabels();
}
