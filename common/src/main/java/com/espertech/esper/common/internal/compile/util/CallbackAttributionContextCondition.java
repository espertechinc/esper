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
package com.espertech.esper.common.internal.compile.util;

public class CallbackAttributionContextCondition implements CallbackAttribution {
    private final int nestingLevel;
    private final boolean startCondition;

    public CallbackAttributionContextCondition(int nestingLevel, boolean startCondition) {
        this.nestingLevel = nestingLevel;
        this.startCondition = startCondition;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public boolean isStartCondition() {
        return startCondition;
    }

    public <T> T accept(CallbackAttributionVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
