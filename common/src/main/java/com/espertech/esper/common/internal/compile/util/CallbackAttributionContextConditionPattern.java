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

public class CallbackAttributionContextConditionPattern implements CallbackAttribution {
    private final int nestingLevel;
    private final boolean startCondition;
    private final short factoryNodeId;

    public CallbackAttributionContextConditionPattern(int nestingLevel, boolean startCondition, short factoryNodeId) {
        this.nestingLevel = nestingLevel;
        this.startCondition = startCondition;
        this.factoryNodeId = factoryNodeId;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public boolean isStartCondition() {
        return startCondition;
    }

    public short getFactoryNodeId() {
        return factoryNodeId;
    }

    public <T> T accept(CallbackAttributionVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
