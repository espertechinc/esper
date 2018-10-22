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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.internal.context.module.StatementLightweight;
import com.espertech.esper.common.internal.context.util.ContextMergeView;

public class ContextControllerStatementDesc {
    private final StatementLightweight lightweight;
    private final ContextMergeView contextMergeView;

    public ContextControllerStatementDesc(StatementLightweight lightweight, ContextMergeView contextMergeView) {
        this.lightweight = lightweight;
        this.contextMergeView = contextMergeView;
    }

    public StatementLightweight getLightweight() {
        return lightweight;
    }

    public ContextMergeView getContextMergeView() {
        return contextMergeView;
    }
}
