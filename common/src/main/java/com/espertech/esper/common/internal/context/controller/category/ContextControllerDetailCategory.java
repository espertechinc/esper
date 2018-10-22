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

import com.espertech.esper.common.internal.context.controller.core.ContextControllerDetail;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

public class ContextControllerDetailCategory implements ContextControllerDetail {

    private ContextControllerDetailCategoryItem[] items;
    private FilterSpecActivatable filterSpecActivatable;

    public ContextControllerDetailCategoryItem[] getItems() {
        return items;
    }

    public void setItems(ContextControllerDetailCategoryItem[] items) {
        this.items = items;
    }

    public FilterSpecActivatable getFilterSpecActivatable() {
        return filterSpecActivatable;
    }

    public void setFilterSpecActivatable(FilterSpecActivatable filterSpecActivatable) {
        this.filterSpecActivatable = filterSpecActivatable;
    }
}
