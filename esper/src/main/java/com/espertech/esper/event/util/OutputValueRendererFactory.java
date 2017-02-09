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
package com.espertech.esper.event.util;

import com.espertech.esper.util.JavaClassHelper;

/**
 * For rendering an output value returned by a property.
 */
public class OutputValueRendererFactory {
    private static OutputValueRenderer jsonStringOutput = new OutputValueRendererJSONString();
    private static OutputValueRenderer xmlStringOutput = new OutputValueRendererXMLString();
    private static OutputValueRenderer baseOutput = new OutputValueRendererBase();

    /**
     * Returns a renderer for an output value.
     *
     * @param type    to render
     * @param options options
     * @return renderer
     */
    protected static OutputValueRenderer getOutputValueRenderer(Class type, RendererMetaOptions options) {
        if (type.isArray()) {
            type = type.getComponentType();
        }
        if (type == String.class ||
                type == Character.class ||
                type == char.class ||
                type.isEnum() ||
                (!JavaClassHelper.isNumeric(type) && JavaClassHelper.getBoxedType(type) != Boolean.class)) {
            if (options.isXmlOutput()) {
                return xmlStringOutput;
            } else {
                return jsonStringOutput;
            }
        } else {
            return baseOutput;
        }
    }
}
