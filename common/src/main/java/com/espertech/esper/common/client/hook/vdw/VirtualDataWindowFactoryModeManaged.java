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

import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategy;

/**
 * Use this class to provide a virtual data window factory wherein there is no need to write code that generates code.
 */
public class VirtualDataWindowFactoryModeManaged implements VirtualDataWindowFactoryMode {
    private InjectionStrategy injectionStrategyFactoryFactory;

    /**
     * Returns the injection strategy for the virtual data window factory
     *
     * @return strategy
     */
    public InjectionStrategy getInjectionStrategyFactoryFactory() {
        return injectionStrategyFactoryFactory;
    }

    /**
     * Sets the injection strategy for the virtual data window factory
     *
     * @param strategy strategy
     * @return itself
     */
    public VirtualDataWindowFactoryModeManaged setInjectionStrategyFactoryFactory(InjectionStrategy strategy) {
        this.injectionStrategyFactoryFactory = strategy;
        return this;
    }
}
