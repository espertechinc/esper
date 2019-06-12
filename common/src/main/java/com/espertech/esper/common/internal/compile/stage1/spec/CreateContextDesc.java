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
package com.espertech.esper.common.internal.compile.stage1.spec;

public class CreateContextDesc {
    private final String contextName;
    private final ContextSpec contextDetail;

    public CreateContextDesc(String contextName, ContextSpec contextDetail) {
        this.contextName = contextName;
        this.contextDetail = contextDetail;
    }

    public String getContextName() {
        return contextName;
    }

    public ContextSpec getContextDetail() {
        return contextDetail;
    }
}
