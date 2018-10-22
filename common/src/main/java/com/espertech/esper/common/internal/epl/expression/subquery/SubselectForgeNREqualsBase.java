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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

/**
 * Strategy for subselects with "=/!=/&gt;&lt; ALL".
 */
public abstract class SubselectForgeNREqualsBase extends SubselectForgeNRBase {
    protected final boolean isNot;
    protected final SimpleNumberCoercer coercer;

    public SubselectForgeNREqualsBase(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNot, SimpleNumberCoercer coercer) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents);
        this.isNot = isNot;
        this.coercer = coercer;
    }
}
