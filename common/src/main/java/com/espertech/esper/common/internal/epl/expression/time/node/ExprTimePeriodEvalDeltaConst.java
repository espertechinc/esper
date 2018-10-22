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
package com.espertech.esper.common.internal.epl.expression.time.node;

import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodDeltaResult;

public interface ExprTimePeriodEvalDeltaConst {
    public long deltaAdd(long fromTime);

    public long deltaSubtract(long fromTime);

    public TimePeriodDeltaResult deltaAddWReference(long fromTime, long reference);
}
