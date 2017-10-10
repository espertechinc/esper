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
package com.espertech.esper.epl.core.resultset.codegen;

import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class ResultSetProcessorCodegenNames {
    public final static String NAME_AGENTINSTANCECONTEXT = "agentInstanceContext";
    public final static String NAME_SELECTEXPRPROCESSOR_MEMBER = "o.selectExprProcessor";
    public final static String NAME_SELECTEXPRPROCESSOR_ARRAY = "o.selectExprProcessorArray";
    public final static String NAME_SELECTEXPRPROCESSOR = "selectExprProcessor";
    public final static String NAME_AGGREGATIONSVC = "aggregationService";
    public final static String NAME_ORDERBYPROCESSOR = "orderByProcessor";
    public final static String NAME_NEWDATA = "newData";
    public final static String NAME_OLDDATA = "oldData";
    public final static String NAME_ISSYNTHESIZE = "isSynthesize";
    public final static String NAME_ISNEWDATA = "isNewData";
    public final static String NAME_JOINSET = "joinset";
    public final static String NAME_VIEWABLE = "viewable";
    public final static String NAME_VIEWEVENTSLIST = "viewEventsList";
    public final static String NAME_JOINEVENTSSET = "joinEventsSet";
    public final static String NAME_RESULTSETVISITOR = "visitor";
    public final static String NAME_HAVINGEVALUATOR_ARRAYNONMEMBER = "havingEvaluatorArray";
    public final static String NAME_HAVINGEVALUATOR_ARRAY_MEMBER = "o." + NAME_HAVINGEVALUATOR_ARRAYNONMEMBER;

    public final static CodegenExpressionRef REF_AGENTINSTANCECONTEXT = new CodegenExpressionRef(NAME_AGENTINSTANCECONTEXT);
    public final static CodegenExpressionRef REF_SELECTEXPRPROCESSOR = new CodegenExpressionRef(NAME_SELECTEXPRPROCESSOR_MEMBER);
    public final static CodegenExpressionRef REF_SELECTEXPRPROCESSOR_ARRAY = new CodegenExpressionRef(NAME_SELECTEXPRPROCESSOR_ARRAY);
    public final static CodegenExpressionRef REF_HAVINGEVALUATOR_ARRAY = new CodegenExpressionRef(NAME_HAVINGEVALUATOR_ARRAY_MEMBER);
    public final static CodegenExpressionRef REF_SELECTEXPRNONMEMBER = new CodegenExpressionRef(NAME_SELECTEXPRPROCESSOR);
    public final static CodegenExpressionRef REF_AGGREGATIONSVC = new CodegenExpressionRef(NAME_AGGREGATIONSVC);
    public final static CodegenExpressionRef REF_ORDERBYPROCESSOR = new CodegenExpressionRef(NAME_ORDERBYPROCESSOR);
    public final static CodegenExpressionRef REF_NEWDATA = ref(NAME_NEWDATA);
    public final static CodegenExpressionRef REF_OLDDATA = ref(NAME_OLDDATA);
    public final static CodegenExpressionRef REF_ISSYNTHESIZE = ref(NAME_ISSYNTHESIZE);
    public final static CodegenExpressionRef REF_ISNEWDATA = ref(NAME_ISNEWDATA);
    public final static CodegenExpressionRef REF_JOINSET = ref(NAME_JOINSET);
    public final static CodegenExpressionRef REF_VIEWABLE = ref(NAME_VIEWABLE);
    public final static CodegenExpressionRef REF_VIEWEVENTSLIST = ref(NAME_VIEWEVENTSLIST);
    public final static CodegenExpressionRef REF_JOINEVENTSSET = ref(NAME_JOINEVENTSSET);
    public final static CodegenExpressionRef REF_RESULTSETVISITOR = ref(NAME_RESULTSETVISITOR);
}
