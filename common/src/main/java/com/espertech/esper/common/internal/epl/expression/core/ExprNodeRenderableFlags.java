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
package com.espertech.esper.common.internal.epl.expression.core;

public class ExprNodeRenderableFlags {
    public final static ExprNodeRenderableFlags DEFAULTFLAGS = new ExprNodeRenderableFlags(true);

    private boolean withStreamPrefix;

    public ExprNodeRenderableFlags(boolean withStreamPrefix) {
        this.withStreamPrefix = withStreamPrefix;
    }

    public boolean isWithStreamPrefix() {
        return withStreamPrefix;
    }

    public void setWithStreamPrefix(boolean withStreamPrefix) {
        this.withStreamPrefix = withStreamPrefix;
    }
}
