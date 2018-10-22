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
package com.espertech.esper.regressionlib.support.filter;

public class PermutationSpec {
    private final boolean all;
    private final int[] specific;

    public PermutationSpec(boolean all) {
        this.all = all;
        this.specific = null;
    }

    public PermutationSpec(int... specific) {
        this.all = false;
        this.specific = specific;
    }

    public boolean isAll() {
        return all;
    }

    public int[] getSpecific() {
        return specific;
    }
}
