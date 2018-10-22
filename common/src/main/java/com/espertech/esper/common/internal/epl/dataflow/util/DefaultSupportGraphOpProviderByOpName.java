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
package com.espertech.esper.common.internal.epl.dataflow.util;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowOperatorProvider;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowOperatorProviderContext;

import java.util.Map;

public class DefaultSupportGraphOpProviderByOpName implements EPDataFlowOperatorProvider {
    private final Map<String, Object> names;

    public DefaultSupportGraphOpProviderByOpName(Map<String, Object> names) {
        this.names = names;
    }

    public Object provide(EPDataFlowOperatorProviderContext context) {
        if (names.containsKey(context.getOperatorName())) {
            return names.get(context.getOperatorName());
        }
        if (context.getFactory() instanceof DefaultSupportSourceOpFactory) {
            DefaultSupportSourceOpFactory factory = (DefaultSupportSourceOpFactory) context.getFactory();
            if (factory.getName() != null && names.containsKey(factory.getName())) {
                return names.get(factory.getName());
            }
        }
        if (context.getFactory() instanceof DefaultSupportCaptureOpFactory) {
            DefaultSupportCaptureOpFactory factory = (DefaultSupportCaptureOpFactory) context.getFactory();
            if (factory.getName() != null && names.containsKey(factory.getName())) {
                return names.get(factory.getName());
            }
        }
        return null;
    }
}
