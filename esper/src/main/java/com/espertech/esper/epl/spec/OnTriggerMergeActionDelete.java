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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

/**
 * Specification for the merge statement delete-part.
 */
public class OnTriggerMergeActionDelete extends OnTriggerMergeAction {
    private static final long serialVersionUID = 8183386154578818969L;

    public OnTriggerMergeActionDelete(ExprNode optionalMatchCond) {
        super(optionalMatchCond);
    }
}

