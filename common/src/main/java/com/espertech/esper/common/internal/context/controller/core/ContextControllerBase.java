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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;

public abstract class ContextControllerBase implements ContextController {
    protected final ContextManagerRealization realization;

    public ContextControllerBase(ContextManagerRealization realization) {
        this.realization = realization;
    }

    public ContextManagerRealization getRealization() {
        return realization;
    }
}
