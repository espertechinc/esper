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
package com.espertech.esper.epl.datetime.interval;

import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.ForgeFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.util.List;
import java.util.TimeZone;

public class IntervalForgeFactory implements ForgeFactory {
    public IntervalForge getForge(StreamTypeService streamTypeService, DatetimeMethodEnum method, String methodNameUsed, List<ExprNode> parameters, TimeZone timeZone, TimeAbacus timeAbacus)
            throws ExprValidationException {
        return new IntervalForgeImpl(method, methodNameUsed, streamTypeService, parameters, timeZone, timeAbacus);
    }

}
