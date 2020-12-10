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
package com.espertech.esper.common.internal.epl.util;

import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;

public class ViewResourceVerifyResult {
    private final ViewResourceDelegateDesc[] descriptors;
    private final FabricCharge fabricCharge;

    public ViewResourceVerifyResult(ViewResourceDelegateDesc[] descriptors, FabricCharge fabricCharge) {
        this.descriptors = descriptors;
        this.fabricCharge = fabricCharge;
    }

    public ViewResourceDelegateDesc[] getDescriptors() {
        return descriptors;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
