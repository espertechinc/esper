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

public class PathExceptionInvalidVisibility extends PathException {

    private static final long serialVersionUID = -8930440966672146718L;

    public PathExceptionInvalidVisibility(PathRegistryObjectType objectType) {
        super("Attempted to add non-path-visibility " + objectType);
    }
}
