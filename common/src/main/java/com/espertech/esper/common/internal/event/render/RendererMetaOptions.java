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
package com.espertech.esper.common.internal.event.render;

import com.espertech.esper.common.client.render.EventPropertyRenderer;
import com.espertech.esper.common.client.render.EventPropertyRendererContext;

/**
 * Options for use by {@link RendererMeta} with rendering metadata.
 */
public class RendererMetaOptions {
    private final boolean preventLooping;
    private final boolean xmlOutput;
    private final EventPropertyRenderer renderer;
    private final EventPropertyRendererContext rendererContext;

    /**
     * Ctor.
     *
     * @param preventLooping  true to prevent looping
     * @param xmlOutput       true for XML output
     * @param rendererContext context
     * @param renderer        renderer
     */
    public RendererMetaOptions(boolean preventLooping, boolean xmlOutput, EventPropertyRenderer renderer, EventPropertyRendererContext rendererContext) {
        this.preventLooping = preventLooping;
        this.xmlOutput = xmlOutput;
        this.renderer = renderer;
        this.rendererContext = rendererContext;
    }

    /**
     * Returns true to prevent looping.
     *
     * @return prevent looping indicator
     */
    public boolean isPreventLooping() {
        return preventLooping;
    }

    /**
     * Returns true for XML output.
     *
     * @return XML output flag
     */
    public boolean isXmlOutput() {
        return xmlOutput;
    }

    public EventPropertyRenderer getRenderer() {
        return renderer;
    }

    public EventPropertyRendererContext getRendererContext() {
        return rendererContext;
    }
}
