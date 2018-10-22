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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;

import java.util.List;

public class SubSelectActivationPlan {
    private final EventType viewableType;
    private final List<ViewFactoryForge> viewForges;
    private final ViewableActivatorForge activator;
    private final StreamSpecCompiled streamSpecCompiled;

    public SubSelectActivationPlan(EventType viewableType, List<ViewFactoryForge> viewForges, ViewableActivatorForge activator, StreamSpecCompiled streamSpecCompiled) {
        this.viewableType = viewableType;
        this.viewForges = viewForges;
        this.activator = activator;
        this.streamSpecCompiled = streamSpecCompiled;
    }

    public EventType getViewableType() {
        return viewableType;
    }

    public List<ViewFactoryForge> getViewForges() {
        return viewForges;
    }

    public ViewableActivatorForge getActivator() {
        return activator;
    }

    public StreamSpecCompiled getStreamSpecCompiled() {
        return streamSpecCompiled;
    }
}
