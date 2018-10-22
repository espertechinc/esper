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
 * Represents a in-subselect evaluation strategy.
 */
public abstract class SubselectForgeNREqualsInBase extends SubselectForgeNRBase {
    protected final boolean isNotIn;
    protected final SimpleNumberCoercer coercer;

    public SubselectForgeNREqualsInBase(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNotIn, SimpleNumberCoercer coercer) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents);
        this.isNotIn = isNotIn;
        this.coercer = coercer;
    }
}
