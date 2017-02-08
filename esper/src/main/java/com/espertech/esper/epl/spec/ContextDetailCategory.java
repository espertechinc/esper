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
package com.espertech.esper.epl.spec;

import com.espertech.esper.filter.FilterSpecCompiled;
import com.espertech.esper.filter.FilterValueSetParam;

import java.util.ArrayList;
import java.util.List;

public class ContextDetailCategory implements ContextDetail {

    private static final long serialVersionUID = 8141827106254268831L;
    private final List<ContextDetailCategoryItem> items;
    private final FilterSpecRaw filterSpecRaw;

    private transient FilterSpecCompiled filterSpecCompiled;
    private transient FilterValueSetParam[][] filterParamsCompiled;

    public ContextDetailCategory(List<ContextDetailCategoryItem> items, FilterSpecRaw filterSpecRaw) {
        this.items = items;
        this.filterSpecRaw = filterSpecRaw;
    }

    public List<FilterSpecCompiled> getContextDetailFilterSpecs() {
        List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>(1);
        filters.add(filterSpecCompiled);
        return filters;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public List<ContextDetailCategoryItem> getItems() {
        return items;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpec) {
        this.filterSpecCompiled = filterSpec;
        this.filterParamsCompiled = filterSpecCompiled.getValueSet(null, null, null).getParameters();
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public FilterValueSetParam[][] getFilterParamsCompiled() {
        return filterParamsCompiled;
    }
}
