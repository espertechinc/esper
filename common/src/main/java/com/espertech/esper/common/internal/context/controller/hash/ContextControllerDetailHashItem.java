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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

public class ContextControllerDetailHashItem {
    private FilterSpecActivatable filterSpecActivatable;
    private ExprFilterSpecLookupable lookupable;

    public FilterSpecActivatable getFilterSpecActivatable() {
        return filterSpecActivatable;
    }

    public void setFilterSpecActivatable(FilterSpecActivatable filterSpecActivatable) {
        this.filterSpecActivatable = filterSpecActivatable;
    }

    public ExprFilterSpecLookupable getLookupable() {
        return lookupable;
    }

    public void setLookupable(ExprFilterSpecLookupable lookupable) {
        this.lookupable = lookupable;
    }
}
