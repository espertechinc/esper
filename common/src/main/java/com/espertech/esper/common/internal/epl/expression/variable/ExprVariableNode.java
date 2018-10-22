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
package com.espertech.esper.common.internal.epl.expression.variable;

import com.espertech.esper.common.internal.epl.expression.core.ExprNodeDeployTimeConst;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

/**
 * Represents a variable in an expression tree.
 */
public interface ExprVariableNode extends ExprNodeDeployTimeConst {
    public String getVariableNameWithSubProp();

    public VariableMetaData getVariableMetadata();
}
