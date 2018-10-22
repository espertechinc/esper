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
package com.espertech.esper.common.internal.epl.pattern.core;

public abstract class EvalNodeBase implements EvalNode {
    private final PatternAgentInstanceContext context;

    protected EvalNodeBase(PatternAgentInstanceContext context) {
        this.context = context;
    }

    public final PatternAgentInstanceContext getContext() {
        return context;
    }
}
