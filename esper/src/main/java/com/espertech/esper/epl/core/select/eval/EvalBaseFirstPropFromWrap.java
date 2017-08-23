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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.event.WrapperEventType;

public abstract class EvalBaseFirstPropFromWrap extends EvalBaseFirstProp {

    protected final WrapperEventType wrapper;

    public EvalBaseFirstPropFromWrap(SelectExprForgeContext selectExprForgeContext, WrapperEventType wrapper) {
        super(selectExprForgeContext, wrapper);
        this.wrapper = wrapper;
    }
}
