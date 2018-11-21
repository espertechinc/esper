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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternObserverSpec;
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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

/**
 * This class represents an observer expression in the evaluation tree representing an pattern expression.
 */
public class EvalObserverForgeNode extends EvalForgeNodeBase {
    private static final Logger log = LoggerFactory.getLogger(EvalObserverForgeNode.class);

    private final PatternObserverSpec patternObserverSpec;
    private ObserverForge observerForge;

    /**
     * Constructor.
     *
     * @param patternObserverSpec is the factory to use to get an observer instance
     * @param attachPatternText whether to attach EPL subexpression text
     */
    public EvalObserverForgeNode(boolean attachPatternText, PatternObserverSpec patternObserverSpec) {
        super(attachPatternText);
        this.patternObserverSpec = patternObserverSpec;
    }

    protected Class typeOfFactory() {
        return EvalObserverFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "observer";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref("node"), "setObserverFactory", observerForge.makeCodegen(method, symbols, classScope));
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
     * @param observerForge is the observer forge
     */
    public void setObserverFactory(ObserverForge observerForge) {
        this.observerForge = observerForge;
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
        observerForge.collectSchedule(schedules);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(patternObserverSpec.getObjectNamespace());
        writer.write(":");
        writer.write(patternObserverSpec.getObjectName());
        writer.write("(");
        ExprNodeUtilityPrint.toExpressionStringParameterList(patternObserverSpec.getObjectParameters(), writer);
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
}
