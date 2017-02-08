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

public class FireAndForgetSpecInsert extends FireAndForgetSpec {
    private static final long serialVersionUID = -2473275073393671915L;
    private final boolean useValuesKeyword;

    public FireAndForgetSpecInsert(boolean useValuesKeyword) {
        this.useValuesKeyword = useValuesKeyword;
    }

    public boolean isUseValuesKeyword() {
        return useValuesKeyword;
    }
}
