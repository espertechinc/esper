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

import java.util.Set;

/**
 * Implement this interface to provide or override the module-uses at compile-time.
 */
public interface ModuleUsesOption {
    /**
     * Returns the module-uses to use or null if none is assigned.
     *
     * @param env the module compile context
     * @return module-uses or null if none needs to be assigned
     */
    Set<String> getValue(ModuleUsesContext env);
}

