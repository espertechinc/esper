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

public class IntoTableSpec implements Serializable {

    private static final long serialVersionUID = -6811039771454719542L;

    private final String name;

    public IntoTableSpec(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
