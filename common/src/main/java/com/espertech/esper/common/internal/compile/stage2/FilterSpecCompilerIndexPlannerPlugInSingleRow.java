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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableFactoryForge;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamConstantForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

public class FilterSpecCompilerIndexPlannerPlugInSingleRow {
    protected static FilterSpecParamForge handlePlugInSingleRow(ExprPlugInSingleRowNode constituent) {
        if (JavaClassHelper.getBoxedType(constituent.getForge().getEvaluationType()) != Boolean.class) {
            return null;
        }
        if (!constituent.getFilterLookupEligible()) {
            return null;
        }
        ExprFilterSpecLookupableFactoryForge lookupable = constituent.getFilterLookupable();
        return new FilterSpecParamConstantForge(lookupable, FilterOperator.EQUAL, true);
    }
}
