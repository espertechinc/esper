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
import com.espertech.esper.epl.spec.FilterSpecRaw;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * This class represents a filter of events in the evaluation tree representing any event expressions.
 */
public class EvalFilterFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = 0L;
    private final FilterSpecRaw rawFilterSpec;
    private final String eventAsName;
    private transient FilterSpecCompiled filterSpec;
    private final Integer consumptionLevel;

    private int eventAsTagNumber = -1;

    /**
     * Constructor.
     *
     * @param filterSpecification specifies the filter properties
     * @param eventAsName         is the name to use for adding matching events to the MatchedEventMap
     *                            table used when indicating truth value of true.
     * @param consumptionLevel    when using @consume
     */
    protected EvalFilterFactoryNode(FilterSpecRaw filterSpecification,
                                    String eventAsName,
                                    Integer consumptionLevel) {
        this.rawFilterSpec = filterSpecification;
        this.eventAsName = eventAsName;
        this.consumptionLevel = consumptionLevel;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        return new EvalFilterNode(agentInstanceContext, this);
    }

    /**
     * Returns the raw (unoptimized/validated) filter definition.
     *
     * @return filter def
     */
    public FilterSpecRaw getRawFilterSpec() {
        return rawFilterSpec;
    }

    /**
     * Returns filter specification.
     *
     * @return filter definition
     */
    public final FilterSpecCompiled getFilterSpec() {
        return filterSpec;
    }

    /**
     * Sets a validated and optimized filter specification
     *
     * @param filterSpec is the optimized filter
     */
    public void setFilterSpec(FilterSpecCompiled filterSpec) {
        this.filterSpec = filterSpec;
    }

    /**
     * Returns the tag for any matching events to this filter, or null since tags are optional.
     *
     * @return tag string for event
     */
    public final String getEventAsName() {
        return eventAsName;
    }

    public Integer getConsumptionLevel() {
        return consumptionLevel;
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("EvalFilterNode rawFilterSpec=" + this.rawFilterSpec);
        buffer.append(" filterSpec=" + this.filterSpec);
        buffer.append(" eventAsName=" + this.eventAsName);
        return buffer.toString();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public int getEventAsTagNumber() {
        return eventAsTagNumber;
    }

    public void setEventAsTagNumber(int eventAsTagNumber) {
        this.eventAsTagNumber = eventAsTagNumber;
    }

    public boolean isStateful() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (getEventAsName() != null) {
            writer.append(getEventAsName());
            writer.append("=");
        }
        writer.append(rawFilterSpec.getEventTypeName());
        if (rawFilterSpec.getFilterExpressions() != null && rawFilterSpec.getFilterExpressions().size() > 0) {
            writer.append("(");
            ExprNodeUtilityCore.toExpressionStringParameterList(rawFilterSpec.getFilterExpressions(), writer);
            writer.append(")");
        }
        if (consumptionLevel != null) {
            writer.append("@consume");
            if (consumptionLevel != 1) {
                writer.append("(");
                writer.append(Integer.toString(consumptionLevel));
                writer.append(")");
            }
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.ATOM;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalFilterFactoryNode.class);
}
