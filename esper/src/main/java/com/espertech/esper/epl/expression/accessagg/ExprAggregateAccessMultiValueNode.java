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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;

public interface ExprAggregateAccessMultiValueNode extends ExprEvaluatorEnumeration {

    public void validatePositionals()
            throws ExprValidationException;

    public abstract AggregationMethodFactory validateAggregationParamsWBinding(ExprValidationContext context, TableMetadataColumnAggregation tableAccessColumn)
            throws ExprValidationException;
}
