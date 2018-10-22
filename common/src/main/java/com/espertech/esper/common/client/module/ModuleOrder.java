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
package com.espertech.esper.common.client.module;

import java.io.Serializable;
import java.util.List;

/**
 * Holds an ordered list of modules considering each module's uses-dependencies on other modules.
 */
public class ModuleOrder implements Serializable {
    private static final long serialVersionUID = 2314846279223748146L;

    private List<Module> ordered;

    /**
     * Cotr.
     *
     * @param ordered list of modules
     */
    public ModuleOrder(List<Module> ordered) {
        this.ordered = ordered;
    }

    /**
     * Returns the list of modules.
     *
     * @return modules
     */
    public List<Module> getOrdered() {
        return ordered;
    }
}
