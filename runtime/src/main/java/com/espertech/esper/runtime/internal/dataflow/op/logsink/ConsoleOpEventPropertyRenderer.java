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
package com.espertech.esper.runtime.internal.dataflow.op.logsink;

import com.espertech.esper.common.client.render.EventPropertyRenderer;
import com.espertech.esper.common.client.render.EventPropertyRendererContext;

import java.util.Arrays;

public class ConsoleOpEventPropertyRenderer implements EventPropertyRenderer {
    public final static ConsoleOpEventPropertyRenderer INSTANCE = new ConsoleOpEventPropertyRenderer();

    public void render(EventPropertyRendererContext context) {
        if (context.getPropertyValue() instanceof Object[]) {
            context.getStringBuilder().append(Arrays.toString((Object[]) context.getPropertyValue()));
        } else {
            context.getDefaultRenderer().render(context.getPropertyValue(), context.getStringBuilder());
        }
    }
}
