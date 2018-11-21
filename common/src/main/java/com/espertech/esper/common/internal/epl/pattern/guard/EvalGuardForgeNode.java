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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.client.soda.GuardEnum;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternGuardSpec;
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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

/**
 * This class represents a guard in the evaluation tree representing an event expressions.
 */
public class EvalGuardForgeNode extends EvalForgeNodeBase {
    private PatternGuardSpec patternGuardSpec;
    private GuardForge guardForge;

    /**
     * Constructor.
     *
     * @param patternGuardSpec - factory for guard construction
     * @param attachPatternText whether to attach EPL subexpression text
     */
    public EvalGuardForgeNode(boolean attachPatternText, PatternGuardSpec patternGuardSpec) {
        super(attachPatternText);
        this.patternGuardSpec = patternGuardSpec;
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
     * @param guardForge is the guard factory
     */
    public void setGuardForge(GuardForge guardForge) {
        this.guardForge = guardForge;
    }

    /**
     * Returns the guard factory.
     *
     * @return guard factory
     */
    public GuardForge getGuardForge() {
        return guardForge;
    }

    public final String toString() {
        return "EvalGuardNode guardForge=" + guardForge +
                "  children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return true;
    }

    protected Class typeOfFactory() {
        return EvalGuardFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "guard";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref("node"), "setChildNode", localMethod(getChildNodes().get(0).makeCodegen(method, symbols, classScope)))
                .exprDotMethod(ref("node"), "setGuardFactory", guardForge.makeCodegen(method, symbols, classScope));
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
        guardForge.collectSchedule(schedules);
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
        ExprNodeUtilityPrint.toExpressionStringParameterList(patternGuardSpec.getObjectParameters(), writer);
        writer.write(")");
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.GUARD_POSTFIX;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalGuardForgeNode.class);
}
