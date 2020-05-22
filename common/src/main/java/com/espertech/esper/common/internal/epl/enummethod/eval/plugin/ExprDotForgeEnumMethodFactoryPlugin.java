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
package com.espertech.esper.common.internal.epl.enummethod.eval.plugin;

import com.espertech.esper.common.client.hook.enummethod.EnumMethodForgeFactory;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeEnumMethod;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeEnumMethodFactory;

public class ExprDotForgeEnumMethodFactoryPlugin implements ExprDotForgeEnumMethodFactory {
    private final EnumMethodForgeFactory forgeFactory;

    public ExprDotForgeEnumMethodFactoryPlugin(EnumMethodForgeFactory forgeFactory) {
        this.forgeFactory = forgeFactory;
    }

    public ExprDotForgeEnumMethod make(int numParameters) {
        return new ExprDotForgeEnumMethodPlugin(forgeFactory);
    }
}
