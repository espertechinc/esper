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
package com.espertech.esper.common.internal.epl.dataflow.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.lang.annotation.Annotation;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.annotation.AnnotationUtil.makeAnnotations;

public class OperatorMetadataDescriptor {
    private Class forgeClass;
    private String operatorPrettyPrint;
    private Annotation[] operatorAnnotations;
    private int numOutputPorts;
    private String operatorName;

    public OperatorMetadataDescriptor() {
    }

    public OperatorMetadataDescriptor(Class forgeClass, String operatorPrettyPrint, Annotation[] operatorAnnotations, int numOutputPorts, String operatorName) {
        this.forgeClass = forgeClass;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.operatorAnnotations = operatorAnnotations;
        this.numOutputPorts = numOutputPorts;
        this.operatorName = operatorName;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(OperatorMetadataDescriptor.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(OperatorMetadataDescriptor.class, "op", newInstance(OperatorMetadataDescriptor.class))
                .exprDotMethod(ref("op"), "setForgeClass", constant(forgeClass))
                .exprDotMethod(ref("op"), "setOperatorPrettyPrint", constant(operatorPrettyPrint))
                .exprDotMethod(ref("op"), "setOperatorAnnotations", operatorAnnotations == null ? constantNull() : localMethod(makeAnnotations(Annotation[].class, operatorAnnotations, method, classScope)))
                .exprDotMethod(ref("op"), "setNumOutputPorts", constant(numOutputPorts))
                .exprDotMethod(ref("op"), "setOperatorName", constant(operatorName))
                .methodReturn(ref("op"));
        return localMethod(method);
    }

    public Class getForgeClass() {
        return forgeClass;
    }

    public String getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }

    public Annotation[] getOperatorAnnotations() {
        return operatorAnnotations;
    }

    public void setForgeClass(Class forgeClass) {
        this.forgeClass = forgeClass;
    }

    public void setOperatorPrettyPrint(String operatorPrettyPrint) {
        this.operatorPrettyPrint = operatorPrettyPrint;
    }

    public void setOperatorAnnotations(Annotation[] operatorAnnotations) {
        this.operatorAnnotations = operatorAnnotations;
    }

    public int getNumOutputPorts() {
        return numOutputPorts;
    }

    public void setNumOutputPorts(int numOutputPorts) {
        this.numOutputPorts = numOutputPorts;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }
}
