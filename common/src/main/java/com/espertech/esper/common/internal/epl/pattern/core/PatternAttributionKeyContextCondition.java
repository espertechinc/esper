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

import com.espertech.esper.common.client.util.NameAccessModifier;

public class PatternAttributionKeyContextCondition implements PatternAttributionKey {
    private final String contextName;
    private final NameAccessModifier contextVisibility;
    private final String moduleName;
    private final int nestingLevel;
    private final boolean startCondition;
    private final boolean keyed;

    public PatternAttributionKeyContextCondition(String contextName, NameAccessModifier contextVisibility, String moduleName, int nestingLevel, boolean startCondition, boolean keyed) {
        this.contextName = contextName;
        this.contextVisibility = contextVisibility;
        this.moduleName = moduleName;
        this.nestingLevel = nestingLevel;
        this.startCondition = startCondition;
        this.keyed = keyed;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public boolean isStartCondition() {
        return startCondition;
    }

    public String getContextName() {
        return contextName;
    }

    public NameAccessModifier getContextVisibility() {
        return contextVisibility;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean isKeyed() {
        return keyed;
    }

    public <T> T accept(PatternAttributionKeyVisitor<T> visitor, short factoryNodeId) {
        return visitor.visit(this, factoryNodeId);
    }
}
