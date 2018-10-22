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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.List;

public class ContextConditionDescriptorFilter implements ContextConditionDescriptor {

    private FilterSpecActivatable filterSpecActivatable;
    private String optionalFilterAsName;

    public FilterSpecActivatable getFilterSpecActivatable() {
        return filterSpecActivatable;
    }

    public void setFilterSpecActivatable(FilterSpecActivatable filterSpecActivatable) {
        this.filterSpecActivatable = filterSpecActivatable;
    }

    public void addFilterSpecActivatable(List<FilterSpecActivatable> activatables) {
        activatables.add(filterSpecActivatable);
    }

    public String getOptionalFilterAsName() {
        return optionalFilterAsName;
    }

    public void setOptionalFilterAsName(String optionalFilterAsName) {
        this.optionalFilterAsName = optionalFilterAsName;
    }
}
