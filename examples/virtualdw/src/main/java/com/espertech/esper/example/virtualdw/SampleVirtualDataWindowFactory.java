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
package com.espertech.esper.example.virtualdw;

import com.espertech.esper.common.client.hook.vdw.VirtualDataWindow;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowContext;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactory;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryContext;

import java.util.Set;

public class SampleVirtualDataWindowFactory implements VirtualDataWindowFactory {

    public void initialize(VirtualDataWindowFactoryContext factoryContext) {
    }

    public VirtualDataWindow create(VirtualDataWindowContext context) {
        return new SampleVirtualDataWindow(context);
    }

    public void destroy() {
        // cleanup can be performed here
    }

    public Set<String> getUniqueKeyPropertyNames() {
        // lets assume there is no unique key property names
        return null;
    }
}
