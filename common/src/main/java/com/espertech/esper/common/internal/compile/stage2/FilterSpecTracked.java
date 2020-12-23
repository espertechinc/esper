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

import com.espertech.esper.common.internal.compile.util.CallbackAttribution;

public class FilterSpecTracked {
    private final CallbackAttribution attribution;
    private final FilterSpecCompiled filterSpecCompiled;

    public FilterSpecTracked(CallbackAttribution attribution, FilterSpecCompiled filterSpecCompiled) {
        this.attribution = attribution;
        this.filterSpecCompiled = filterSpecCompiled;
    }

    public CallbackAttribution getAttribution() {
        return attribution;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }
}
