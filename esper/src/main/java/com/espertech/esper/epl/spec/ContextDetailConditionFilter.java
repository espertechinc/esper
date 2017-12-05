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

import com.espertech.esper.filterspec.FilterSpecCompiled;

public class ContextDetailConditionFilter implements ContextDetailCondition {

    private static final long serialVersionUID = -8522513130123236000L;
    private final FilterSpecRaw filterSpecRaw;
    private final String optionalFilterAsName;

    private transient FilterSpecCompiled filterSpecCompiled;

    public ContextDetailConditionFilter(FilterSpecRaw filterSpecRaw, String optionalFilterAsName) {
        this.filterSpecRaw = filterSpecRaw;
        this.optionalFilterAsName = optionalFilterAsName;
    }

    public FilterSpecRaw getFilterSpecRaw() {
        return filterSpecRaw;
    }

    public String getOptionalFilterAsName() {
        return optionalFilterAsName;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public void setFilterSpecCompiled(FilterSpecCompiled filterSpecCompiled) {
        this.filterSpecCompiled = filterSpecCompiled;
    }
}
