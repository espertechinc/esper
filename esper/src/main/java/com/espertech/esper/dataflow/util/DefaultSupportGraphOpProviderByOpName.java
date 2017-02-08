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

import java.util.Map;

public class DefaultSupportGraphOpProviderByOpName implements EPDataFlowOperatorProvider {
    private final Map<String, Object> names;

    public DefaultSupportGraphOpProviderByOpName(Map<String, Object> names) {
        this.names = names;
    }

    public Object provide(EPDataFlowOperatorProviderContext context) {
        return names.get(context.getOperatorName());
    }
}
