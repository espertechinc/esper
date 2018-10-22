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
 * Provides the environment to {@link AccessModifierNamedWindowOption}.
 */
public class AccessModifierNamedWindowContext extends StatementOptionContextBase {

    private final String namedWindowName;

    /**
     * Ctor
     *
     * @param base            statement info
     * @param namedWindowName named window name
     */
    public AccessModifierNamedWindowContext(StatementBaseInfo base, String namedWindowName) {
        super(base);
        this.namedWindowName = namedWindowName;
    }

    /**
     * Returns the named window name
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }
}
