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
package com.espertech.esper.epl.join.plan;

public class CoercionDesc {

    private boolean coerce;
    private Class[] coercionTypes;

    public CoercionDesc(boolean coerce, Class[] coercionTypes) {
        this.coerce = coerce;
        this.coercionTypes = coercionTypes;
    }

    public boolean isCoerce() {
        return coerce;
    }

    public Class[] getCoercionTypes() {
        return coercionTypes;
    }
}
