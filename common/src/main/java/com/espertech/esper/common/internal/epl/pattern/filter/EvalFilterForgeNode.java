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
package com.espertech.esper.common.internal.epl.pattern.filter;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.stage1.spec.FilterSpecRaw;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionPrecedenceEnum;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a filter of events in the evaluation tree representing any event expressions.
 */
public class EvalFilterForgeNode extends EvalForgeNodeBase {
    private final FilterSpecRaw rawFilterSpec;
    private final String eventAsName;
    private transient FilterSpecCompiled filterSpec;
    private final Integer consumptionLevel;

    private int eventAsTagNumber = -1;

    /**
     * Constructor.
     *
     * @param attachPatternText whether to attach EPL subexpression text
     * @param filterSpecification specifies the filter properties
     * @param eventAsName         is the name to use for adding matching events to the MatchedEventMap
     *                            table used when indicating truth value of true.
     * @param consumptionLevel    when using @consume
     */
    public EvalFilterForgeNode(boolean attachPatternText,
                               FilterSpecRaw filterSpecification,
                               String eventAsName,
                               Integer consumptionLevel) {
        super(attachPatternText);
        this.rawFilterSpec = filterSpecification;
        this.eventAsName = eventAsName;
        this.consumptionLevel = consumptionLevel;
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
    public final FilterSpecCompiled getFilterSpecCompiled() {
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
            ExprNodeUtilityPrint.toExpressionStringParameterList(rawFilterSpec.getFilterExpressions(), writer);
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

    protected Class typeOfFactory() {
        return EvalFilterFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "filter";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref("node"), "setFilterSpec", localMethod(filterSpec.makeCodegen(method, symbols, classScope)))
                .exprDotMethod(ref("node"), "setEventAsName", constant(eventAsName))
                .exprDotMethod(ref("node"), "setConsumptionLevel", constant(consumptionLevel))
                .exprDotMethod(ref("node"), "setEventAsTagNumber", constant(eventAsTagNumber));
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
        filters.add(filterSpec);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalFilterForgeNode.class);
}
