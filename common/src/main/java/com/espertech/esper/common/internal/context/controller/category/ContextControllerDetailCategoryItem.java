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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlan;

public class ContextControllerDetailCategoryItem {

    private String name;
    private FilterSpecPlan filterPlan;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FilterSpecPlan getFilterPlan() {
        return filterPlan;
    }

    public void setFilterPlan(FilterSpecPlan filterPlan) {
        this.filterPlan = filterPlan;
    }
}
