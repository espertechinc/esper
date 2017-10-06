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
package com.espertech.esper.epl.agg.codegen;

import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class AggregationServiceCodegenNames {
    public final static String NAME_ENGINEIMPORTSVC = "engineImportService";
    public final static CodegenExpressionRef REF_ENGINEIMPORTSVC = ref(NAME_ENGINEIMPORTSVC);
    public final static String NAME_ISSUBQUERY = "isSubquery";
    public final static CodegenExpressionRef REF_ISSUBQUERY = ref(NAME_ISSUBQUERY);
    public final static String NAME_SUBQUERYNUMBER = "subqueryNumber";
    public final static CodegenExpressionRef REF_SUBQUERYNUMBER = ref(NAME_SUBQUERYNUMBER);
    public final static String NAME_GROUPKEY = "groupKey";
    public final static CodegenExpressionRef REF_GROUPKEY = ref(NAME_GROUPKEY);
    public final static String NAME_AGENTINSTANCEID = "agentInstanceId";
    public final static String NAME_ROLLUPLEVEL = "rollupLevel";
    public final static CodegenExpressionRef REF_ROLLUPLEVEL = ref(NAME_ROLLUPLEVEL);
    public final static String NAME_CALLBACK = "callback";
    public final static CodegenExpressionRef REF_CALLBACK = ref(NAME_CALLBACK);
    public final static String NAME_AGGVISITOR = "visitor";
    public final static CodegenExpressionRef REF_AGGVISITOR = ref(NAME_AGGVISITOR);
    public final static String NAME_COLUMN = "column";
    public final static CodegenExpressionRef REF_COLUMN = ref(NAME_COLUMN);
    public final static String CLASSNAME_AGGREGATIONSERVICEFACTORY = "AggFactory";
    public final static String CLASSNAME_AGGREGATIONSERVICE = "AggSvc";
    public final static String CLASSNAME_AGGREGATIONROW_TOP = "AggRowTop";
    public final static String CLASSNAME_AGGREGATIONROW_LVL = "AggRowLvl";
}
