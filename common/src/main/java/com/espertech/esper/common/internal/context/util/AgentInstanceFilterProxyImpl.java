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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

public class AgentInstanceFilterProxyImpl implements AgentInstanceFilterProxy {

    private Function<AgentInstanceContext, IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]>> generator;
    private Map<FilterSpecActivatable, FilterValueSetParam[][]> addendumMap;

    public AgentInstanceFilterProxyImpl(Function<AgentInstanceContext, IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]>> generator) {
        this.generator = generator;
    }

    public FilterValueSetParam[][] getAddendumFilters(FilterSpecActivatable filterSpec, AgentInstanceContext agentInstanceContext) {
        if (addendumMap == null) {
            addendumMap = generator.apply(agentInstanceContext);
            if (addendumMap.isEmpty()) {
                addendumMap = Collections.emptyMap();
            }
            generator = null;
        }
        return addendumMap.get(filterSpec);
    }
}
