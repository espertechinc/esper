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
 * Provides the environment to {@link AccessModifierContextOption}.
 */
public class AccessModifierContextContext extends StatementOptionContextBase {

    private final String contextName;

    /**
     * Ctor.
     *
     * @param base        statement info
     * @param contextName context name
     */
    public AccessModifierContextContext(StatementBaseInfo base, String contextName) {
        super(base);
        this.contextName = contextName;
    }

    /**
     * Returns the context name.
     *
     * @return context name
     */
    public String getContextName() {
        return contextName;
    }
}
