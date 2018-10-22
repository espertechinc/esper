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
package com.espertech.esper.regressionlib.support.context;

import com.espertech.esper.common.client.context.ContextPartitionSelectorById;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SupportSelectorById implements ContextPartitionSelectorById {
    private final Set<Integer> ids;

    public SupportSelectorById(Set<Integer> ids) {
        this.ids = ids;
    }

    public SupportSelectorById(int id) {
        this.ids = Collections.singleton(id);
    }

    public Set<Integer> getContextPartitionIds() {
        return ids;
    }

    public static SupportSelectorById fromSetOfAll(int num) {
        Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < num; i++) {
            set.add(i);
        }
        return new SupportSelectorById(set);
    }
}
