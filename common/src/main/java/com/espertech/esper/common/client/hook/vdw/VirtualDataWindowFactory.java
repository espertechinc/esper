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

/**
 * Factory for virtual data windows.
 */
public interface VirtualDataWindowFactory {
    /**
     * Invoked after instantiation of the factory, exactly once per named window.
     *
     * @param initializeContext provides contextual information such as event type, named window name and parameters.
     */
    void initialize(VirtualDataWindowFactoryContext initializeContext);

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
    VirtualDataWindow create(VirtualDataWindowContext context);

    /**
     * Invoked upon undeployment of the virtual data window.
     */
    void destroy();
}
