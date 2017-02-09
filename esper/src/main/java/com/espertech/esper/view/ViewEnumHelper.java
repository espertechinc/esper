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

import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.epl.spec.PluggableObjectType;

/**
 * Helper producing a repository of built-in views.
 */
public class ViewEnumHelper {
    private final static PluggableObjectCollection BUILTIN_VIEWS;

    static {
        BUILTIN_VIEWS = new PluggableObjectCollection();
        for (ViewEnum viewEnum : ViewEnum.values()) {
            BUILTIN_VIEWS.addObject(viewEnum.getNamespace(), viewEnum.getName(), viewEnum.getFactoryClass(), PluggableObjectType.VIEW);
        }
    }

    /**
     * Returns a collection of plug-in views.
     *
     * @return built-in view definitions
     */
    public static PluggableObjectCollection getBuiltinViews() {
        return BUILTIN_VIEWS;
    }
}
