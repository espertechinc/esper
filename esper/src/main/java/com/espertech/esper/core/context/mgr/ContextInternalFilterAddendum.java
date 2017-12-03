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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.IdentityHashMap;
import java.util.Map;

public class ContextInternalFilterAddendum {
    private final IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum;

    public ContextInternalFilterAddendum() {
        filterAddendum = new IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]>();
    }

    public FilterValueSetParam[][] getFilterAddendum(FilterSpecCompiled filterSpecCompiled) {
        return filterAddendum.get(filterSpecCompiled);
    }

    public IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> getFilterAddendum() {
        return filterAddendum;
    }

    public ContextInternalFilterAddendum deepCopy() {
        ContextInternalFilterAddendum copy = new ContextInternalFilterAddendum();
        for (Map.Entry<FilterSpecCompiled, FilterValueSetParam[][]> entry : filterAddendum.entrySet()) {
            FilterValueSetParam[][] copy2Dim = new FilterValueSetParam[entry.getValue().length][];
            copy.filterAddendum.put(entry.getKey(), copy2Dim);
            for (int i = 0; i < entry.getValue().length; i++) {
                FilterValueSetParam[] copyList = new FilterValueSetParam[entry.getValue()[i].length];
                copy2Dim[i] = copyList;
                System.arraycopy(entry.getValue()[i], 0, copyList, 0, copyList.length);
            }
            copy.filterAddendum.put(entry.getKey(), copy2Dim);
        }
        return copy;
    }
}
