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
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

/**
 * Superclass of all nodes in an evaluation tree representing an event pattern expression.
 * Follows the Composite pattern. Child nodes do not carry references to parent nodes, the tree
 * is unidirectional.
 */
public interface EvalForgeNode {
    /**
     * Adds a child node.
     *
     * @param childNode is the child evaluation tree node to add
     */
    void addChildNode(EvalForgeNode childNode);

    /**
     * Returns list of child nodes
     *
     * @return list of child nodes
     */
    List<EvalForgeNode> getChildNodes();

    void addChildNodes(Collection<EvalForgeNode> childNodes);

    void setFactoryNodeId(short factoryNodeId);

    short getFactoryNodeId();

    void setAudit(boolean audit);

    /**
     * Returns precendence.
     *
     * @return precendence
     */
    PatternExpressionPrecedenceEnum getPrecedence();

    /**
     * Write expression considering precendence.
     *
     * @param writer           to use
     * @param parentPrecedence precendence
     */
    void toEPL(StringWriter writer, PatternExpressionPrecedenceEnum parentPrecedence);

    CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules);
}
