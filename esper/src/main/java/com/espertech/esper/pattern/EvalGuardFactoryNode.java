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
package com.espertech.esper.pattern;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.spec.PatternGuardSpec;
import com.espertech.esper.pattern.guard.GuardEnum;
import com.espertech.esper.pattern.guard.GuardFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * This class represents a guard in the evaluation tree representing an event expressions.
 */
public class EvalGuardFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = -6426206281275755119L;
    private PatternGuardSpec patternGuardSpec;
    private transient GuardFactory guardFactory;

    /**
     * Constructor.
     *
     * @param patternGuardSpec - factory for guard construction
     */
    protected EvalGuardFactoryNode(PatternGuardSpec patternGuardSpec) {
        this.patternGuardSpec = patternGuardSpec;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalGuardNode(agentInstanceContext, this, child);
    }

    /**
     * Returns the guard object specification to use for instantiating the guard factory and guard.
     *
     * @return guard specification
     */
    public PatternGuardSpec getPatternGuardSpec() {
        return patternGuardSpec;
    }

    /**
     * Supplies the guard factory to the node.
     *
     * @param guardFactory is the guard factory
     */
    public void setGuardFactory(GuardFactory guardFactory) {
        this.guardFactory = guardFactory;
    }

    /**
     * Returns the guard factory.
     *
     * @return guard factory
     */
    public GuardFactory getGuardFactory() {
        return guardFactory;
    }

    public final String toString() {
        return "EvalGuardNode guardFactory=" + guardFactory +
                "  children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return true;
    }

    public String toPrecedenceFreeEPL() {
        StringWriter writer = new StringWriter();
        toPrecedenceFreeEPL(writer);
        return writer.toString();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        getChildNodes().get(0).toEPL(writer, getPrecedence());
        if (patternGuardSpec.getObjectNamespace().equals(GuardEnum.WHILE_GUARD.getNamespace()) &&
                patternGuardSpec.getObjectName().equals(GuardEnum.WHILE_GUARD.getName())) {
            writer.write(" while ");
        } else {
            writer.write(" where ");
            writer.write(patternGuardSpec.getObjectNamespace());
            writer.write(":");
            writer.write(patternGuardSpec.getObjectName());
        }
        writer.write("(");
        ExprNodeUtilityCore.toExpressionStringParameterList(patternGuardSpec.getObjectParameters(), writer);
        writer.write(")");
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.GUARD_POSTFIX;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalGuardFactoryNode.class);
}
