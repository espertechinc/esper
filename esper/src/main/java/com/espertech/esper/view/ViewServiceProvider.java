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
package com.espertech.esper.view;

/**
 * Static factory for implementations of the {@link com.espertech.esper.view.ViewService} interface.
 */
public final class ViewServiceProvider {
    /**
     * Creates an implementation of the ViewService interface.
     *
     * @return implementation
     */
    public static ViewService newService() {
        return new ViewServiceImpl();
    }
}
