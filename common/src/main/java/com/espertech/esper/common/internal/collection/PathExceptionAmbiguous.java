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

public class PathExceptionAmbiguous extends PathException {
    private static final long serialVersionUID = -5453494323933761455L;

    public PathExceptionAmbiguous(String name, PathRegistryObjectType objectType) {
        super(objectType.getPrefix() + " " + objectType.getName() + " by name '" + name + "' is exported by multiple modules");
    }
}
