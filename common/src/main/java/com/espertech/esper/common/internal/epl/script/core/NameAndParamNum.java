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
package com.espertech.esper.common.internal.epl.script.core;

import java.util.List;

public class NameAndParamNum {
    private final static NameAndParamNum[] EMPTY_ARRAY = new NameAndParamNum[0];

    private final String name;
    private final int paramNum;

    public NameAndParamNum(String name, int paramNum) {
        this.name = name;
        this.paramNum = paramNum;
    }

    public String getName() {
        return name;
    }

    public int getParamNum() {
        return paramNum;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameAndParamNum that = (NameAndParamNum) o;

        if (paramNum != that.paramNum) return false;
        return name.equals(that.name);
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + paramNum;
        return result;
    }

    public static NameAndParamNum[] toArray(List<NameAndParamNum> pathScripts) {
        if (pathScripts.isEmpty()) {
            return EMPTY_ARRAY;
        }
        return pathScripts.toArray(new NameAndParamNum[pathScripts.size()]);
    }

    public String toString() {
        return name + " (" + paramNum + " parameters)";
    }
}
