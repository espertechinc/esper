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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableFactory;

import java.util.List;

public class FilterSpecCompiledDesc {
    private final FilterSpecCompiled filterSpecCompiled;
    private final List<StmtClassForgableFactory> additionalForgeables;

    public FilterSpecCompiledDesc(FilterSpecCompiled filterSpecCompiled, List<StmtClassForgableFactory> additionalForgeables) {
        this.filterSpecCompiled = filterSpecCompiled;
        this.additionalForgeables = additionalForgeables;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public List<StmtClassForgableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
