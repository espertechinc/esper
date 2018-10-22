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
package com.espertech.esper.common.internal.context.activator;

public class ViewableActivatorFactoryImpl implements ViewableActivatorFactory {
    public final static ViewableActivatorFactoryImpl INSTANCE = new ViewableActivatorFactoryImpl();

    private ViewableActivatorFactoryImpl() {
    }

    public ViewableActivatorFilter createFilter() {
        return new ViewableActivatorFilter();
    }

    public ViewableActivatorPattern createPattern() {
        return new ViewableActivatorPattern();
    }

    public ViewableActivatorNamedWindow createNamedWindow() {
        return new ViewableActivatorNamedWindow();
    }
}
