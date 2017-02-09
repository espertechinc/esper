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
package com.espertech.esper.dataflow.util;

import com.espertech.esper.client.dataflow.EPDataFlowOperatorProvider;
import com.espertech.esper.client.dataflow.EPDataFlowOperatorProviderContext;

public class DefaultSupportGraphOpProvider implements EPDataFlowOperatorProvider {
    private final Object[] ops;

    public DefaultSupportGraphOpProvider(Object op) {
        this.ops = new Object[]{op};
    }

    public DefaultSupportGraphOpProvider(Object... ops) {
        this.ops = ops;
    }

    public Object provide(EPDataFlowOperatorProviderContext context) {
        for (Object op : ops) {
            if (context.getOperatorName().equals(op.getClass().getSimpleName())) {
                return op;
            }
        }
        return null;
    }
}
