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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeableUtil;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.assemble.BaseAssemblyNodeFactory;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanNodeForgeVisitor;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlan;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlanForge;
import com.espertech.esper.common.internal.util.IndentWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Query plan for executing a set of lookup instructions and assembling an end result via
 * a set of assembly instructions.
 */
public class LookupInstructionQueryPlanNodeForge extends QueryPlanNodeForge {
    private final int rootStream;
    private final String rootStreamName;
    private final int numStreams;
    private final List<LookupInstructionPlanForge> lookupInstructions;
    private final boolean[] requiredPerStream;
    private final List<BaseAssemblyNodeFactory> assemblyInstructionFactories;

    /**
     * Ctor.
     *
     * @param rootStream                   is the stream supplying the lookup event
     * @param rootStreamName               is the name of the stream supplying the lookup event
     * @param numStreams                   is the number of streams
     * @param lookupInstructions           is a list of lookups to perform
     * @param requiredPerStream            indicates which streams are required and which are optional in the lookup
     * @param assemblyInstructionFactories is the bottom-up assembly factory nodes to assemble a lookup result nodes
     */
    public LookupInstructionQueryPlanNodeForge(int rootStream,
                                               String rootStreamName,
                                               int numStreams,
                                               boolean[] requiredPerStream,
                                               List<LookupInstructionPlanForge> lookupInstructions,
                                               List<BaseAssemblyNodeFactory> assemblyInstructionFactories) {
        this.rootStream = rootStream;
        this.rootStreamName = rootStreamName;
        this.lookupInstructions = lookupInstructions;
        this.numStreams = numStreams;
        this.requiredPerStream = requiredPerStream;
        this.assemblyInstructionFactories = assemblyInstructionFactories;
    }

    @Override
    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        for (LookupInstructionPlanForge plan : lookupInstructions) {
            plan.addIndexes(usedIndexes);
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = makeInstructions(assemblyInstructionFactories, parent, symbols, classScope);
        return newInstance(LookupInstructionQueryPlanNode.class,
                constant(rootStream),
                constant(rootStreamName),
                constant(numStreams),
                constant(requiredPerStream),
                CodegenMakeableUtil.makeArray("lookupInstructions", LookupInstructionPlan.class, lookupInstructions.toArray(new LookupInstructionPlanForge[0]), this.getClass(), parent, symbols, classScope),
                localMethod(method));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param factories factories
     * @param parents   parents indexes
     * @param children  children indexes
     */
    public static void assembleFactoriesIntoTree(BaseAssemblyNodeFactory[] factories, int[] parents, int[][] children) {
        for (int i = 0; i < parents.length; i++) {
            if (parents[i] != -1) {
                factories[i].setParent(factories[parents[i]]);
            }
        }
        for (int i = 0; i < children.length; i++) {
            for (int child = 0; child < children[i].length; child++) {
                factories[i].addChild(factories[children[i][child]]);
            }
        }
    }

    private CodegenMethod makeInstructions(List<BaseAssemblyNodeFactory> factories, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(BaseAssemblyNodeFactory[].class, this.getClass(), classScope);

        int[] parents = new int[factories.size()];
        int[][] children = new int[factories.size()][];
        for (int i = 0; i < factories.size(); i++) {
            BaseAssemblyNodeFactory factory = factories.get(i);
            parents[i] = factory.getParentNode() == null ? -1 : findFactoryChecked(factory.getParentNode(), factories);
            children[i] = new int[factory.getChildNodes().size()];
            for (int child = 0; child < factory.getChildNodes().size(); child++) {
                children[i][child] = findFactoryChecked(factory.getChildNodes().get(child), factories);
            }
        }

        method.getBlock()
                .declareVar(BaseAssemblyNodeFactory[].class, "factories", CodegenMakeableUtil.makeArray("assemblyInstructions", BaseAssemblyNodeFactory.class, factories.toArray(new BaseAssemblyNodeFactory[0]), this.getClass(), parent, symbols, classScope))
                .staticMethod(LookupInstructionQueryPlanNodeForge.class, "assembleFactoriesIntoTree", ref("factories"), constant(parents), constant(children))
                .methodReturn(ref("factories"));

        return method;
    }

    protected void print(IndentWriter writer) {
        writer.println("LookupInstructionQueryPlanNode" +
                " rootStream=" + rootStream +
                " requiredPerStream=" + Arrays.toString(requiredPerStream));

        writer.incrIndent();
        for (int i = 0; i < lookupInstructions.size(); i++) {
            writer.println("lookup step " + i);
            writer.incrIndent();
            lookupInstructions.get(i).print(writer);
            writer.decrIndent();
        }
        writer.decrIndent();

        writer.incrIndent();
        for (int i = 0; i < assemblyInstructionFactories.size(); i++) {
            writer.println("assembly step " + i);
            writer.incrIndent();
            assemblyInstructionFactories.get(i).print(writer);
            writer.decrIndent();
        }
        writer.decrIndent();
    }

    public int getRootStream() {
        return rootStream;
    }

    public String getRootStreamName() {
        return rootStreamName;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public List<LookupInstructionPlanForge> getLookupInstructions() {
        return lookupInstructions;
    }

    public boolean[] getRequiredPerStream() {
        return requiredPerStream;
    }

    public List<BaseAssemblyNodeFactory> getAssemblyInstructionFactories() {
        return assemblyInstructionFactories;
    }

    public void accept(QueryPlanNodeForgeVisitor visitor) {
        visitor.visit(this);
    }

    private int findFactoryChecked(BaseAssemblyNodeFactory node, List<BaseAssemblyNodeFactory> factories) {
        int index = factories.indexOf(node);
        if (index == -1) {
            throw new UnsupportedOperationException("Assembly factory not found among list");
        }
        return index;
    }
}
