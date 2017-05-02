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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffectorProvider;
import com.espertech.esper.epl.variable.VariableService;

import java.util.List;

/**
 * Represents an Dot-operator expression, for use when "(expression).method(...).method(...)"
 */
public interface ExprDotNode extends ExprNode, FilterExprAnalyzerAffectorProvider  {
    String FILTERINDEX_NAMED_PARAMETER = "filterindex";

    Integer getStreamReferencedIfAny();
    List<ExprChainedSpec> getChainSpec();
    String isVariableOpGetName(VariableService variableService);
}

