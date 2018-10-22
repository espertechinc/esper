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

import com.espertech.esper.common.client.context.ContextPartitionSelectorHash;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SupportSelectorByHashCode implements ContextPartitionSelectorHash {

    private final Set<Integer> hashes;

    public SupportSelectorByHashCode(Set<Integer> hashes) {
        this.hashes = hashes;
    }

    public SupportSelectorByHashCode(int single) {
        this.hashes = Collections.singleton(single);
    }

    public Set<Integer> getHashes() {
        return hashes;
    }

    public static SupportSelectorByHashCode fromSetOfAll(int num) {
        Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < num; i++) {
            set.add(i);
        }
        return new SupportSelectorByHashCode(set);
    }
}
