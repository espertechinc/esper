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
 * A factory that the runtime invokes at deployment time to obtain the virtual data window factory.
 */
public interface VirtualDataWindowFactoryFactory {
    /**
     * Return the virtual data window factory
     *
     * @param ctx context information
     * @return factory
     */
    VirtualDataWindowFactory createFactory(VirtualDataWindowFactoryFactoryContext ctx);
}
