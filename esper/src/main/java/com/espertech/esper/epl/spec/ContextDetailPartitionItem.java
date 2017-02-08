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

import java.io.Serializable;
import java.util.List;

public class ContextDetailPartitionItem implements Serializable {

    private static final long serialVersionUID = -4009763999241702138L;
    private final FilterSpecRaw filterSpecRaw;
    private final List<String> propertyNames;

    private transient FilterSpecCompiled filterSpecCompiled;
    private transient FilterValueSetParam[][] parametersCompiled;

    public ContextDetailPartitionItem(FilterSpecRaw filterSpecRaw, List<String> propertyNames) {
        this.filterSpecRaw = filterSpecRaw;
        this.propertyNames = propertyNames;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpecCompiled) {
        this.filterSpecCompiled = filterSpecCompiled;
        this.parametersCompiled = filterSpecCompiled.getValueSet(null, null, null).getParameters();
    }

    public FilterValueSetParam[][] getParametersCompiled() {
        return parametersCompiled;
    }
}
