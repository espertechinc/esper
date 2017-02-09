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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.assemble.BaseAssemblyNode;
import com.espertech.esper.epl.join.assemble.BaseAssemblyNodeFactory;
import com.espertech.esper.epl.join.assemble.ResultAssembler;
import com.espertech.esper.epl.join.rep.Node;
import com.espertech.esper.epl.join.rep.RepositoryImpl;
import com.espertech.esper.util.IndentWriter;

import java.util.*;

/**
 * Execution for a set of lookup instructions and for a set of result assemble instructions to perform
 * joins and construct a complex result.
 */
@SuppressWarnings({"StringContatenationInLoop"})
public class LookupInstructionExecNode extends ExecNode {
    private final int rootStream;
    private final String rootStreamName;
    private final int numStreams;
    private final boolean[] requiredPerStream;
    private final LookupInstructionExec[] lookupInstructions;
    private final BaseAssemblyNode[] assemblyInstructions;
    private final MyResultAssembler myResultAssembler;
    private int requireResultsInstruction;

    /**
     * Ctor.
     *
     * @param rootStream                   is the stream supplying the lookup event
     * @param rootStreamName               is the name of the stream supplying the lookup event
     * @param numStreams                   is the number of streams
     * @param lookupInstructions           is a list of lookups to perform
     * @param requiredPerStream            indicates which streams are required and which are optional in the lookup
     * @param assemblyInstructionFactories factories for assembly
     */
    public LookupInstructionExecNode(int rootStream,
                                     String rootStreamName,
                                     int numStreams,
                                     LookupInstructionExec[] lookupInstructions,
                                     boolean[] requiredPerStream,
                                     List<BaseAssemblyNodeFactory> assemblyInstructionFactories) {
        this.rootStream = rootStream;
        this.rootStreamName = rootStreamName;
        this.numStreams = numStreams;
        this.lookupInstructions = lookupInstructions;
        this.requiredPerStream = requiredPerStream;

        // We have a list of factories that are pointing to each other in a tree, i.e.:
        // F1 (->F3), F2 (->F3), F3
        Map<BaseAssemblyNodeFactory, BaseAssemblyNode> nodes = new IdentityHashMap<BaseAssemblyNodeFactory, BaseAssemblyNode>();
        for (BaseAssemblyNodeFactory factory : assemblyInstructionFactories) {
            BaseAssemblyNode node = factory.makeAssemblerUnassociated();
            nodes.put(factory, node);
        }

        // re-associate each node after allocation
        for (Map.Entry<BaseAssemblyNodeFactory, BaseAssemblyNode> nodeWithFactory : nodes.entrySet()) {
            BaseAssemblyNodeFactory parentFactory = nodeWithFactory.getKey().getParentNode();
            if (parentFactory != null) {
                BaseAssemblyNode parent = nodes.get(parentFactory);
                nodeWithFactory.getValue().setParentAssembler(parent);
            }
            for (BaseAssemblyNodeFactory childNodeFactory : nodeWithFactory.getKey().getChildNodes()) {
                BaseAssemblyNode child = nodes.get(childNodeFactory);
                nodeWithFactory.getValue().addChild(child);
            }
        }

        this.assemblyInstructions = new BaseAssemblyNode[assemblyInstructionFactories.size()];
        for (int i = 0; i < assemblyInstructionFactories.size(); i++) {
            this.assemblyInstructions[i] = nodes.get(assemblyInstructionFactories.get(i));
        }

        myResultAssembler = new MyResultAssembler(rootStream);
        assemblyInstructions[assemblyInstructions.length - 1].setParentAssembler(myResultAssembler);

        // Determine up to which instruction we are dealing with optional results.
        // When dealing with optional results we don't do fast exists if we find no lookup results
        requireResultsInstruction = 1;  // we always require results from the very first lookup
        for (int i = 1; i < lookupInstructions.length; i++) {
            int fromStream = lookupInstructions[i].getFromStream();
            if (requiredPerStream[fromStream]) {
                requireResultsInstruction = i + 1;      // require results as long as the from-stream is a required stream
            } else {
                break;
            }
        }
    }

    public void process(EventBean lookupEvent, EventBean[] prefillPath, Collection<EventBean[]> resultFinalRows, ExprEvaluatorContext exprEvaluatorContext) {
        RepositoryImpl repository = new RepositoryImpl(rootStream, lookupEvent, numStreams);
        boolean processOptional = true;

        for (int i = 0; i < requireResultsInstruction; i++) {
            LookupInstructionExec currentInstruction = lookupInstructions[i];
            boolean hasResults = currentInstruction.process(repository, exprEvaluatorContext);

            // no results, check what to do
            if (!hasResults) {
                // If there was a required stream, we are done.
                if (currentInstruction.hasRequiredStream()) {
                    return;
                }

                // If this is the first stream and there are no results, we are done with lookups
                if (i == 0) {
                    processOptional = false;  // go to result processing
                }
            }
        }

        if (processOptional) {
            for (int i = requireResultsInstruction; i < lookupInstructions.length; i++) {
                LookupInstructionExec currentInstruction = lookupInstructions[i];
                currentInstruction.process(repository, exprEvaluatorContext);
            }
        }

        // go over the assembly instruction set
        List<Node>[] results = repository.getNodesPerStream();

        // no results - need to execute the very last instruction/top node
        if (results == null) {
            BaseAssemblyNode lastAssemblyNode = assemblyInstructions[assemblyInstructions.length - 1];
            lastAssemblyNode.init(null);
            lastAssemblyNode.process(null, resultFinalRows, lookupEvent);
            return;
        }

        // we have results - execute all instructions
        BaseAssemblyNode assemblyNode;
        for (int i = 0; i < assemblyInstructions.length; i++) {
            assemblyNode = assemblyInstructions[i];
            assemblyNode.init(results);
        }
        for (int i = 0; i < assemblyInstructions.length; i++) {
            assemblyNode = assemblyInstructions[i];
            assemblyNode.process(results, resultFinalRows, lookupEvent);
        }
    }

    public void print(IndentWriter writer) {
        writer.println("LookupInstructionExecNode" +
                " rootStream=" + rootStream +
                " name=" + rootStreamName +
                " requiredPerStream=" + Arrays.toString(requiredPerStream));

        writer.incrIndent();
        for (int i = 0; i < lookupInstructions.length; i++) {
            writer.println("lookup inst node " + i);
            writer.incrIndent();
            lookupInstructions[i].print(writer);
            writer.decrIndent();
        }
        writer.decrIndent();

        writer.incrIndent();
        for (int i = 0; i < assemblyInstructions.length; i++) {
            writer.println("assembly inst node " + i);
            writer.incrIndent();
            assemblyInstructions[i].print(writer);
            writer.decrIndent();
        }
        writer.decrIndent();
    }

    /**
     * Receives result rows posted by result set assembly nodes.
     */
    public static class MyResultAssembler implements ResultAssembler {
        private final int rootStream;

        /**
         * Ctor.
         *
         * @param rootStream is the root stream for which we get results
         */
        public MyResultAssembler(int rootStream) {
            this.rootStream = rootStream;
        }

        public void result(EventBean[] row, int fromStreamNum, EventBean myEvent, Node myNode, Collection<EventBean[]> resultFinalRows, EventBean resultRootEvent) {
            row[rootStream] = resultRootEvent;
            resultFinalRows.add(row);
        }
    }
}
