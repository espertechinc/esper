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

import com.espertech.esper.client.context.ContextPartitionSelectorSegmented;

import java.util.Collections;
import java.util.List;

public class SupportSelectorPartitioned implements ContextPartitionSelectorSegmented {
    private final List<Object[]> keys;

    public SupportSelectorPartitioned(List<Object[]> keys) {
        this.keys = keys;
    }

    public SupportSelectorPartitioned(Object[] keys) {
        this.keys = Collections.singletonList(keys);
    }

    public SupportSelectorPartitioned(Object singleKey) {
        this.keys = Collections.singletonList(new Object[]{singleKey});
    }

    public List<Object[]> getPartitionKeys() {
        return keys;
    }
}

