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

public class FilterSpecAttributionContextCondition implements FilterSpecAttribution {
    private final int nestingLevel;
    private final boolean startCondition;

    public FilterSpecAttributionContextCondition(int nestingLevel, boolean startCondition) {
        this.nestingLevel = nestingLevel;
        this.startCondition = startCondition;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public boolean isStartCondition() {
        return startCondition;
    }

    public <T> T accept(FilterSpecAttributionVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
