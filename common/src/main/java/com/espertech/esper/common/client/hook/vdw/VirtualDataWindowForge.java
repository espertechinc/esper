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
package com.espertech.esper.common.client.hook.vdw;

import java.util.Set;

/**
 * Factory for {@link VirtualDataWindow}.
 * <p>
 * Register an implementation of this interface with the runtimebefore use:
 * configuration.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWFactory.class.getName());
 */
public interface VirtualDataWindowForge {
    /**
     * Invoked once after instantiation of the forge, exactly once per named window.
     *
     * @param initializeContext provides contextual information such as event type, named window name and parameters.
     */
    void initialize(VirtualDataWindowForgeContext initializeContext);

    /**
     * Describes to the compiler how it should manage code for the virtual data window factory.
     *
     * @return mode object
     */
    VirtualDataWindowFactoryMode getFactoryMode();

    /**
     * Return the names of properties that taken together (combined, composed, not individually) are the unique keys of a row,
     * return null if there are no unique keys that can be identified.
     *
     * @return set of unique key property names
     */
    public Set<String> getUniqueKeyPropertyNames();
}
