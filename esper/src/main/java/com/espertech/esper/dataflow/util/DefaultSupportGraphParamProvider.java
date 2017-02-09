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

import com.espertech.esper.client.dataflow.EPDataFlowOperatorParameterProvider;
import com.espertech.esper.client.dataflow.EPDataFlowOperatorParameterProviderContext;

import java.util.Map;

public class DefaultSupportGraphParamProvider implements EPDataFlowOperatorParameterProvider {
    private final Map<String, Object> params;

    public DefaultSupportGraphParamProvider(Map<String, Object> params) {
        this.params = params;
    }

    public Object provide(EPDataFlowOperatorParameterProviderContext context) {
        return params.get(context.getParameterName());
    }
}
