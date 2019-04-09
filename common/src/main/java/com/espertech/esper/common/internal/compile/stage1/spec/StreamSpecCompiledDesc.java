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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableFactory;

import java.util.List;

public class StreamSpecCompiledDesc {
    private final StreamSpecCompiled streamSpecCompiled;
    private final List<StmtClassForgableFactory> additionalForgeables;

    public StreamSpecCompiledDesc(StreamSpecCompiled streamSpecCompiled, List<StmtClassForgableFactory> additionalForgeables) {
        this.streamSpecCompiled = streamSpecCompiled;
        this.additionalForgeables = additionalForgeables;
    }

    public StreamSpecCompiled getStreamSpecCompiled() {
        return streamSpecCompiled;
    }

    public List<StmtClassForgableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
