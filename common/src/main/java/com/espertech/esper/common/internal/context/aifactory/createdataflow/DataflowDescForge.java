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
package com.espertech.esper.common.internal.context.aifactory.createdataflow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.compile.stage3.StmtForgeMethodResult;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.dataflow.realize.LogicalChannel;
import com.espertech.esper.common.internal.epl.dataflow.util.OperatorMetadataDescriptor;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class DataflowDescForge {

    private final String dataflowName;
    private final Map<String, EventType> declaredTypes;
    private final Map<Integer, OperatorMetadataDescriptor> operatorMetadata;
    private final Set<Integer> operatorBuildOrder;
    private final Map<Integer, DataFlowOperatorForge> operatorFactories;
    private final List<LogicalChannel> logicalChannels;
    private final List<StmtForgeMethodResult> forgables;
    private final List<StmtClassForgeableFactory> additionalForgables;

    public DataflowDescForge(String dataflowName, Map<String, EventType> declaredTypes, Map<Integer, OperatorMetadataDescriptor> operatorMetadata, Set<Integer> operatorBuildOrder, Map<Integer, DataFlowOperatorForge> operatorFactories, List<LogicalChannel> logicalChannels, List<StmtForgeMethodResult> forgables, List<StmtClassForgeableFactory> additionalForgables) {
        this.dataflowName = dataflowName;
        this.declaredTypes = declaredTypes;
        this.operatorMetadata = operatorMetadata;
        this.operatorBuildOrder = operatorBuildOrder;
        this.operatorFactories = operatorFactories;
        this.logicalChannels = logicalChannels;
        this.forgables = forgables;
        this.additionalForgables = additionalForgables;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(DataflowDesc.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVarNewInstance(DataflowDesc.EPTYPE, "df")
                .exprDotMethod(ref("df"), "setDataflowName", constant(dataflowName))
                .exprDotMethod(ref("df"), "setDeclaredTypes", makeTypes(declaredTypes, method, symbols, classScope))
                .exprDotMethod(ref("df"), "setOperatorMetadata", makeOpMeta(operatorMetadata, method, symbols, classScope))
                .exprDotMethod(ref("df"), "setOperatorBuildOrder", makeOpBuildOrder(operatorBuildOrder, method, symbols, classScope))
                .exprDotMethod(ref("df"), "setOperatorFactories", makeOpFactories(operatorFactories, method, symbols, classScope))
                .exprDotMethod(ref("df"), "setLogicalChannels", makeOpChannels(logicalChannels, method, symbols, classScope))
                .methodReturn(ref("df"));
        return localMethod(method);
    }

    public Map<Integer, DataFlowOperatorForge> getOperatorFactories() {
        return operatorFactories;
    }

    public List<StmtForgeMethodResult> getForgables() {
        return forgables;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgables() {
        return additionalForgables;
    }

    private static CodegenExpression makeOpChannels(List<LogicalChannel> logicalChannels, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.LIST.getEPType(), DataflowDescForge.class, classScope);
        method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "chnl", newInstance(EPTypePremade.ARRAYLIST.getEPType(), constant(logicalChannels.size())));
        for (LogicalChannel channel : logicalChannels) {
            method.getBlock().exprDotMethod(ref("chnl"), "add", channel.make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("chnl"));
        return localMethod(method);
    }

    private static CodegenExpression makeOpBuildOrder(Set<Integer> operatorBuildOrder, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.LINKEDHASHSET.getEPType(), DataflowDescForge.class, classScope);
        method.getBlock().declareVar(EPTypePremade.LINKEDHASHSET.getEPType(), "order", newInstance(EPTypePremade.LINKEDHASHSET.getEPType(), constant(CollectionUtil.capacityHashMap(operatorBuildOrder.size()))));
        for (Integer entry : operatorBuildOrder) {
            method.getBlock().exprDotMethod(ref("order"), "add", constant(entry));
        }
        method.getBlock().methodReturn(ref("order"));
        return localMethod(method);
    }

    private static CodegenExpression makeOpFactories(Map<Integer, DataFlowOperatorForge> operatorFactories, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), DataflowDescForge.class, classScope);
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "fac", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(operatorFactories.size()))));
        for (Map.Entry<Integer, DataFlowOperatorForge> entry : operatorFactories.entrySet()) {
            method.getBlock().exprDotMethod(ref("fac"), "put", constant(entry.getKey()), entry.getValue().make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("fac"));
        return localMethod(method);
    }

    private static CodegenExpression makeOpMeta(Map<Integer, OperatorMetadataDescriptor> operatorMetadata, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), DataflowDescForge.class, classScope);
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "op", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(operatorMetadata.size()))));
        for (Map.Entry<Integer, OperatorMetadataDescriptor> entry : operatorMetadata.entrySet()) {
            method.getBlock().exprDotMethod(ref("op"), "put", constant(entry.getKey()), entry.getValue().make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("op"));
        return localMethod(method);
    }

    private static CodegenExpression makeTypes(Map<String, EventType> declaredTypes, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), DataflowDescForge.class, classScope);
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "types", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(declaredTypes.size()))));
        for (Map.Entry<String, EventType> entry : declaredTypes.entrySet()) {
            method.getBlock().exprDotMethod(ref("types"), "put", constant(entry.getKey()), EventTypeUtility.resolveTypeCodegen(entry.getValue(), symbols.getAddInitSvc(method)));
        }
        method.getBlock().methodReturn(ref("types"));
        return localMethod(method);
    }
}
