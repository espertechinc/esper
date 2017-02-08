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
package com.espertech.esper.supportregression.virtualdw;

import com.espertech.esper.client.hook.VirtualDataWindow;
import com.espertech.esper.client.hook.VirtualDataWindowContext;
import com.espertech.esper.client.hook.VirtualDataWindowFactory;
import com.espertech.esper.client.hook.VirtualDataWindowFactoryContext;

import java.util.Set;

public class SupportVirtualDWInvalidFactory implements VirtualDataWindowFactory {

    public void initialize(VirtualDataWindowFactoryContext factoryContext) {
    }

    public VirtualDataWindow create(VirtualDataWindowContext context) {
        return new SupportVirtualDWInvalid();
    }

    public void destroyAllContextPartitions() {
    }

    public Set<String> getUniqueKeyPropertyNames() {
        return null;
    }
}
