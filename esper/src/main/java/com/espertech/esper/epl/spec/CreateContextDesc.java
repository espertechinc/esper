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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

public class CreateContextDesc implements Serializable {

    private static final long serialVersionUID = -5318225626899036861L;
    private final String contextName;
    private final ContextDetail contextDetail;

    public CreateContextDesc(String contextName, ContextDetail contextDetail) {
        this.contextName = contextName;
        this.contextDetail = contextDetail;
    }

    public String getContextName() {
        return contextName;
    }

    public ContextDetail getContextDetail() {
        return contextDetail;
    }
}
