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
package com.espertech.esper.compiler.client.option;

import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;

/**
 * Provides the environment to {@link AccessModifierInlinedClassContext}.
 */
public class AccessModifierInlinedClassContext extends StatementOptionContextBase {

    private final String inlinedClassName;

    /**
     * Ctor
     *
     * @param base         statement info
     * @param inlinedClassName returns the name of the inlined class
     */
    public AccessModifierInlinedClassContext(StatementBaseInfo base, String inlinedClassName) {
        super(base);
        this.inlinedClassName = inlinedClassName;
    }

    /**
     * Returns the inlined-class name
     *
     * @return the inlined-class name
     */
    public String getInlinedClassName() {
        return inlinedClassName;
    }
}
