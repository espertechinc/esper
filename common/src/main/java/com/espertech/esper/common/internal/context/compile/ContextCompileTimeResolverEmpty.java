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
package com.espertech.esper.common.internal.context.compile;

public class ContextCompileTimeResolverEmpty implements ContextCompileTimeResolver {

    public final static ContextCompileTimeResolverEmpty INSTANCE = new ContextCompileTimeResolverEmpty();

    private ContextCompileTimeResolverEmpty() {
    }

    public ContextMetaData getContextInfo(String contextName) {
        return null;
    }
}
