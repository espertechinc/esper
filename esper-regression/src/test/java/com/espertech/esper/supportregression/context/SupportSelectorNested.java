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
package com.espertech.esper.supportregression.context;

import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.context.ContextPartitionSelectorNested;

import java.util.Collections;
import java.util.List;

public class SupportSelectorNested implements ContextPartitionSelectorNested {
    private List<ContextPartitionSelector[]> selectors;

    public SupportSelectorNested(ContextPartitionSelector s0, ContextPartitionSelector s1) {
        this(new ContextPartitionSelector[]{s0, s1});
    }

    public SupportSelectorNested(ContextPartitionSelector[] selectors) {
        this.selectors = Collections.singletonList(selectors);
    }

    public SupportSelectorNested(List<ContextPartitionSelector[]> selectors) {
        this.selectors = selectors;
    }

    public List<ContextPartitionSelector[]> getSelectors() {
        return selectors;
    }
}
