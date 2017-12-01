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
import com.espertech.esper.epl.spec.PatternObserverSpec;
import com.espertech.esper.pattern.observer.ObserverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;


/**
 * This class represents an observer expression in the evaluation tree representing an pattern expression.
 */
public class EvalObserverFactoryNode extends EvalNodeFactoryBase {
    private static final Logger log = LoggerFactory.getLogger(EvalObserverFactoryNode.class);

    private static final long serialVersionUID = 7130273585111632791L;
    private final PatternObserverSpec patternObserverSpec;
    private transient ObserverFactory observerFactory;

    /**
     * Constructor.
     *
     * @param patternObserverSpec is the factory to use to get an observer instance
     */
    protected EvalObserverFactoryNode(PatternObserverSpec patternObserverSpec) {
        this.patternObserverSpec = patternObserverSpec;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        return new EvalObserverNode(agentInstanceContext, this);
    }

    /**
     * Returns the observer object specification to use for instantiating the observer factory and observer.
     *
     * @return observer specification
     */
    public PatternObserverSpec getPatternObserverSpec() {
        return patternObserverSpec;
    }

    /**
     * Supplies the observer factory to the node.
     *
     * @param observerFactory is the observer factory
     */
    public void setObserverFactory(ObserverFactory observerFactory) {
        this.observerFactory = observerFactory;
    }

    /**
     * Returns the observer factory.
     *
     * @return factory for observer instances
     */
    public ObserverFactory getObserverFactory() {
        return observerFactory;
    }

    public final String toString() {
        return "EvalObserverNode observerFactory=" + observerFactory +
                "  children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(patternObserverSpec.getObjectNamespace());
        writer.write(":");
        writer.write(patternObserverSpec.getObjectName());
        writer.write("(");
        ExprNodeUtilityCore.toExpressionStringParameterList(patternObserverSpec.getObjectParameters(), writer);
        writer.write(")");
    }

    public String toPrecedenceFreeEPL() {
        StringWriter writer = new StringWriter();
        toPrecedenceFreeEPL(writer);
        return writer.toString();
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.ATOM;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return observerFactory.isNonRestarting();
    }
}
