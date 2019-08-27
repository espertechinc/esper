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
package com.espertech.esper.common.internal.epl.datetime.interval;

import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodProviderForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;

import java.util.List;

public class IntervalForgeFactory implements DatetimeMethodProviderForgeFactory {
    public IntervalForge getForge(StreamTypeService streamTypeService, DatetimeMethodDesc method, String methodNameUsed, List<ExprNode> parameters, TimeAbacus timeAbacus, TableCompileTimeResolver tableCompileTimeResolver)
        throws ExprValidationException {
        return new IntervalForgeImpl(method, methodNameUsed, streamTypeService, parameters, timeAbacus, tableCompileTimeResolver);
    }
}
