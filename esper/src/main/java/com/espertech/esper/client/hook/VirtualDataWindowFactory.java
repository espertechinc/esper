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
package com.espertech.esper.client.hook;

import java.util.Set;

/**
 * Factory for {@link VirtualDataWindow}.
 * <p>
 * Register an implementation of this interface with the engine before use:
 * configuration.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWFactory.class.getName());
 */
public interface VirtualDataWindowFactory {

    /**
     * Invoked once after instantiation of the factory, exactly once per named window.
     *
     * @param factoryContext factory context provides contextual information such as event type, named window name and parameters.
     */
    public void initialize(VirtualDataWindowFactoryContext factoryContext);

    /**
     * Invoked for each context partition (or once if not using contexts),
     * return a virtual data window to handle the specific event type, named window or paramaters
     * as provided in the context.
     * <p>
     * This method is invoked for each named window instance after the initialize method.
     * If using context partitions, the method is invoked once per context partition per named window.
     * </p>
     *
     * @param context provides contextual information such as event type, named window name and parameters
     *                and including context partition information
     * @return virtual data window
     */
    public VirtualDataWindow create(VirtualDataWindowContext context);

    /**
     * Invoked to indicate the named window is destroyed.
     * <p>
     * This method is invoked once per named window (and not once per context partition).
     * </p>
     * <p>
     * For reference, the VirtualDataWindow destroy method is called once per context partition,
     * before this method is invoked.
     * </p>
     */
    public void destroyAllContextPartitions();

    /**
     * Return the names of properties that taken together (combined, composed, not individually) are the unique keys of a row,
     * return null if there are no unique keys that can be identified.
     *
     * @return set of unique key property names
     */
    public Set<String> getUniqueKeyPropertyNames();

}
