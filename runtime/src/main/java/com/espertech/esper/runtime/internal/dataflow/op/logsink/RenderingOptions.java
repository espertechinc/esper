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

import com.espertech.esper.common.client.render.JSONRenderingOptions;
import com.espertech.esper.common.client.render.XMLRenderingOptions;

public class RenderingOptions {
    private static XMLRenderingOptions xmlOptions;
    private static JSONRenderingOptions jsonOptions;

    static {
        xmlOptions = new XMLRenderingOptions();
        xmlOptions.setPreventLooping(true);
        xmlOptions.setRenderer(ConsoleOpEventPropertyRenderer.INSTANCE);

        jsonOptions = new JSONRenderingOptions();
        jsonOptions.setPreventLooping(true);
        jsonOptions.setRenderer(ConsoleOpEventPropertyRenderer.INSTANCE);
    }

    public static XMLRenderingOptions getXmlOptions() {
        return xmlOptions;
    }

    public static void setXmlOptions(XMLRenderingOptions xmlOptions) {
        RenderingOptions.xmlOptions = xmlOptions;
    }

    public static JSONRenderingOptions getJsonOptions() {
        return jsonOptions;
    }

    public static void setJsonOptions(JSONRenderingOptions jsonOptions) {
        RenderingOptions.jsonOptions = jsonOptions;
    }
}
