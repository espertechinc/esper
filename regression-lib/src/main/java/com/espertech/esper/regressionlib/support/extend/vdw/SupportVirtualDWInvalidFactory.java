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
package com.espertech.esper.regressionlib.support.extend.vdw;

import com.espertech.esper.common.client.hook.vdw.VirtualDataWindow;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowContext;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactory;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryContext;

public class SupportVirtualDWInvalidFactory implements VirtualDataWindowFactory {
    public void initialize(VirtualDataWindowFactoryContext initializeContext) {

    }

    public VirtualDataWindow create(VirtualDataWindowContext context) {
        return new SupportVirtualDWInvalid();
    }

    public void destroy() {
    }
}
