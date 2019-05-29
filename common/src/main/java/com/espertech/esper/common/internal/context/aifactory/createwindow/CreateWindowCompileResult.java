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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseSpecRaw;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class CreateWindowCompileResult {
    private final FilterSpecCompiled filterSpecCompiled;
    private final SelectClauseSpecRaw selectClauseSpecRaw;
    private final EventType asEventType;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public CreateWindowCompileResult(FilterSpecCompiled filterSpecCompiled, SelectClauseSpecRaw selectClauseSpecRaw, EventType asEventType, List<StmtClassForgeableFactory> additionalForgeables) {
        this.filterSpecCompiled = filterSpecCompiled;
        this.selectClauseSpecRaw = selectClauseSpecRaw;
        this.asEventType = asEventType;
        this.additionalForgeables = additionalForgeables;
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }

    public SelectClauseSpecRaw getSelectClauseSpecRaw() {
        return selectClauseSpecRaw;
    }

    public EventType getAsEventType() {
        return asEventType;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
