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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.client.type.EPTypeClass;

public class CoercionDesc {

    private boolean coerce;
    private EPTypeClass[] coercionTypes;

    public CoercionDesc(boolean coerce, EPTypeClass[] coercionTypes) {
        this.coerce = coerce;
        this.coercionTypes = coercionTypes;
    }

    public boolean isCoerce() {
        return coerce;
    }

    public EPTypeClass[] getCoercionTypes() {
        return coercionTypes;
    }
}
