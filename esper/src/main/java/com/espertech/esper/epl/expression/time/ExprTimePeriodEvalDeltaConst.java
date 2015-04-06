/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression.time;

public interface ExprTimePeriodEvalDeltaConst
{
    public long deltaMillisecondsAdd(long fromTime);
    public long deltaMillisecondsSubtract(long fromTime);
    public ExprTimePeriodEvalDeltaResult deltaMillisecondsAddWReference(long fromTime, long reference);
    public boolean equalsTimePeriod(ExprTimePeriodEvalDeltaConst timeDeltaComputation);
}
