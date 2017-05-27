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

import com.espertech.esper.client.context.ContextPartitionSelectorCategory;

import java.util.Collections;
import java.util.Set;

public class SupportSelectorCategory implements ContextPartitionSelectorCategory {
    private Set<String> labels;

    public SupportSelectorCategory(Set<String> labels) {
        this.labels = labels;
    }

    public SupportSelectorCategory(String label) {
        if (label == null) {
            labels = Collections.emptySet();
        } else {
            labels = Collections.singleton(label);
        }
    }

    public Set<String> getLabels() {
        return labels;
    }
}
