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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;

public class EnumMethodDesc {
    private final String enumMethodName;
    private final EnumMethodEnum enumMethod;
    private final ExprDotForgeEnumMethodFactory factory;
    private final DotMethodFP[] parameters;

    public EnumMethodDesc(String methodName, EnumMethodEnum enumMethod, ExprDotForgeEnumMethodFactory factory, DotMethodFP[] parameters) {
        this.enumMethodName = methodName;
        this.enumMethod = enumMethod;
        this.factory = factory;
        this.parameters = parameters;
    }

    public EnumMethodEnum getEnumMethod() {
        return enumMethod;
    }

    public ExprDotForgeEnumMethodFactory getFactory() {
        return factory;
    }

    public DotMethodFP[] getFootprints() {
        return parameters;
    }

    public String getEnumMethodName() {
        return enumMethodName;
    }
}
