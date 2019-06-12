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
package com.espertech.esper.common.internal.collection;

import com.espertech.esper.common.internal.util.StringValue;

public class PathExceptionAlreadyRegistered extends PathException {
    private static final long serialVersionUID = -2816268359908454331L;

    public PathExceptionAlreadyRegistered(String name, PathRegistryObjectType objectType, String moduleName) {
        super(objectType.getPrefix() + " " + objectType.getName() + " by name '" + name + "' has already been created for module '" + StringValue.unnamedWhenNullOrEmpty(moduleName) + "'");
    }
}

