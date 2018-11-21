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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class EvalForgeNodeBase implements EvalForgeNode {
    private final List<EvalForgeNode> childNodes;
    protected short factoryNodeId;
    protected boolean audit;
    private boolean attachPatternText;

    protected abstract Class typeOfFactory();

    protected abstract String nameOfFactory();

    protected abstract void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public abstract void toPrecedenceFreeEPL(StringWriter writer);

    /**
     * Constructor creates a list of child nodes.
     * @param attachPatternText whether to attach EPL subexpression text
     */
    public EvalForgeNodeBase(boolean attachPatternText) {
        childNodes = new ArrayList<>();
        this.attachPatternText = attachPatternText;
    }

    /**
     * Adds a child node.
     *
     * @param childNode is the child evaluation tree node to add
     */
    public void addChildNode(EvalForgeNode childNode) {
        childNodes.add(childNode);
    }

    public void addChildNodes(Collection<EvalForgeNode> childNodesToAdd) {
        childNodes.addAll(childNodesToAdd);
    }

    /**
     * Returns list of child nodes.
     *
     * @return list of child nodes
     */
    public List<EvalForgeNode> getChildNodes() {
        return childNodes;
    }

    public void setFactoryNodeId(short factoryNodeId) {
        this.factoryNodeId = factoryNodeId;
    }

    public short getFactoryNodeId() {
        return factoryNodeId;
    }

    public boolean isAudit() {
        return audit;
    }

    public void setAudit(boolean audit) {
        this.audit = audit;
    }

    public final void toEPL(StringWriter writer, PatternExpressionPrecedenceEnum parentPrecedence) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer);
        }
    }

    public final CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(typeOfFactory(), this.getClass(), classScope);
        method.getBlock()
                .declareVar(typeOfFactory(), "node", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add(nameOfFactory()))
                .exprDotMethod(ref("node"), "setFactoryNodeId", constant(factoryNodeId));
        if (audit || classScope.isInstrumented() || attachPatternText) {
            StringWriter writer = new StringWriter();
            toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
            String expressionText = writer.toString();
            method.getBlock().exprDotMethod(ref("node"), "setTextForAudit", constant(expressionText));
        }
        inlineCodegen(method, symbols, classScope);
        method.getBlock().methodReturn(ref("node"));
        return method;
    }

}
