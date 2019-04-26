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

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class StatementSpecCompiledDesc {

    private final StatementSpecCompiled compiled;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public StatementSpecCompiledDesc(StatementSpecCompiled compiled, List<StmtClassForgeableFactory> additionalForgeables) {
        this.compiled = compiled;
        this.additionalForgeables = additionalForgeables;
    }

    public StatementSpecCompiled getCompiled() {
        return compiled;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
