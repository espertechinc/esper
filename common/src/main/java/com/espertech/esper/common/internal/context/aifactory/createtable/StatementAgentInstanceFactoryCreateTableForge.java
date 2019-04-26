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
package com.espertech.esper.common.internal.context.aifactory.createtable;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInnerClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationClassNames;
import com.espertech.esper.common.internal.epl.agg.core.AggregationCodegenRowLevelDesc;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryCompiler;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.table.compiletime.TableAccessAnalysisResult;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnPairAggAccess;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnPairAggMethod;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnPairPlainCol;
import com.espertech.esper.common.internal.epl.table.core.TableMetadataInternalEventToPublic;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

public class StatementAgentInstanceFactoryCreateTableForge {
    private final String className;
    private final String tableName;
    private final TableAccessAnalysisResult plan;

    public StatementAgentInstanceFactoryCreateTableForge(String className, String tableName, TableAccessAnalysisResult plan) {
        this.className = className;
        this.tableName = tableName;
        this.plan = plan;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        // add aggregation row+factory+serde as inner classes
        AggregationClassNames aggregationClassNames = new AggregationClassNames();
        List<CodegenInnerClass> inners = AggregationServiceFactoryCompiler.makeTable(AggregationCodegenRowLevelDesc.fromTopOnly(plan.getAggDesc()), this.getClass(), classScope, aggregationClassNames, className);
        classScope.addInnerClasses(inners);

        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateTable.class, this.getClass(), classScope);

        CodegenExpression primaryKeyGetter = MultiKeyCodegen.codegenGetterMayMultiKey(plan.getInternalEventType(), plan.getPrimaryKeyGetters(), plan.getPrimaryKeyTypes(), null, plan.getPrimaryKeyMultikeyClasses(), method, classScope);
        CodegenExpression fafTransform = MultiKeyCodegen.codegenMultiKeyFromArrayTransform(plan.getPrimaryKeyMultikeyClasses(), method, classScope);
        CodegenExpression intoTableTransform = MultiKeyCodegen.codegenMultiKeyFromMultiKeyTransform(plan.getPrimaryKeyMultikeyClasses(), method, classScope);

        method.getBlock()
            .declareVar(StatementAgentInstanceFactoryCreateTable.class, "saiff", newInstance(StatementAgentInstanceFactoryCreateTable.class))
            .exprDotMethod(ref("saiff"), "setTableName", constant(tableName))
            .exprDotMethod(ref("saiff"), "setPublicEventType", EventTypeUtility.resolveTypeCodegen(plan.getPublicEventType(), symbols.getAddInitSvc(method)))
            .exprDotMethod(ref("saiff"), "setEventToPublic", makeEventToPublic(method, symbols, classScope))
            .exprDotMethod(ref("saiff"), "setAggregationRowFactory", CodegenExpressionBuilder.newInstance(aggregationClassNames.getRowFactoryTop(), ref("this")))
            .exprDotMethod(ref("saiff"), "setAggregationSerde", CodegenExpressionBuilder.newInstance(aggregationClassNames.getRowSerdeTop(), ref("this")))
            .exprDotMethod(ref("saiff"), "setPrimaryKeyGetter", primaryKeyGetter)
            .exprDotMethod(ref("saiff"), "setPrimaryKeySerde", plan.getPrimaryKeyMultikeyClasses().getExprMKSerde(method, classScope))
            .exprDotMethod(ref("saiff"), "setPropertyForges", DataInputOutputSerdeForge.codegenArray(plan.getInternalEventTypePropertySerdes(), method, classScope, exprDotMethod(symbols.getAddInitSvc(method), EPStatementInitServices.GETEVENTTYPERESOLVER)))
            .exprDotMethod(ref("saiff"), "setPrimaryKeyObjectArrayTransform", fafTransform)
            .exprDotMethod(ref("saiff"), "setPrimaryKeyIntoTableTransform", intoTableTransform)
            .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", ref("saiff"))
            .methodReturn(ref("saiff"));
        return method;
    }

    private CodegenExpression makeEventToPublic(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TableMetadataInternalEventToPublic.class, this.getClass(), classScope);
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = classScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(plan.getPublicEventType(), EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass clazz = newAnonymousClass(method.getBlock(), TableMetadataInternalEventToPublic.class);

        CodegenMethod convert = CodegenMethod.makeParentNode(EventBean.class, this.getClass(), classScope).addParam(EventBean.class, "event").addParam(ExprForgeCodegenNames.PARAMS);
        clazz.addMethod("convert", convert);
        convert.getBlock()
            .declareVar(Object[].class, "data", exprDotMethod(ref("this"), "convertToUnd", ref("event"), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT))
            .methodReturn(exprDotMethod(factory, "adapterForTypedObjectArray", ref("data"), eventType));

        CodegenMethod convertToUnd = CodegenMethod.makeParentNode(Object[].class, this.getClass(), classScope).addParam(EventBean.class, "event").addParam(ExprForgeCodegenNames.PARAMS);
        clazz.addMethod("convertToUnd", convertToUnd);
        convertToUnd.getBlock()
            .declareVar(Object[].class, "props", exprDotMethod(cast(ObjectArrayBackedEventBean.class, ref("event")), "getProperties"))
            .declareVar(Object[].class, "data", newArrayByLength(Object.class, constant(plan.getPublicEventType().getPropertyNames().length)));
        for (TableMetadataColumnPairPlainCol plain : plan.getColsPlain()) {
            convertToUnd.getBlock().assignArrayElement(ref("data"), constant(plain.getDest()), arrayAtIndex(ref("props"), constant(plain.getSource())));
        }
        if (plan.getColsAggMethod().length > 0 || plan.getColsAccess().length > 0) {
            convertToUnd.getBlock().declareVar(AggregationRow.class, "row", cast(AggregationRow.class, arrayAtIndex(ref("props"), constant(0))));
            int count = 0;

            for (TableMetadataColumnPairAggMethod aggMethod : plan.getColsAggMethod()) {
                // Code: data[method.getDest()] = row.getMethods()[count++].getValue();
                convertToUnd.getBlock().assignArrayElement(ref("data"), constant(aggMethod.getDest()), exprDotMethod(ref("row"), "getValue", constant(count), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                count++;
            }

            for (TableMetadataColumnPairAggAccess aggAccess : plan.getColsAccess()) {
                // Code: data[method.getDest()] = row.getMethods()[count++].getValue();
                convertToUnd.getBlock().assignArrayElement(ref("data"), constant(aggAccess.getDest()), exprDotMethod(ref("row"), "getValue", constant(count), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                count++;
            }
        }
        convertToUnd.getBlock().methodReturn(ref("data"));

        method.getBlock().methodReturn(clazz);
        return localMethod(method);
    }
}
