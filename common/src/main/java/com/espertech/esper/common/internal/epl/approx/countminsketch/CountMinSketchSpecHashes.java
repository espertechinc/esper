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
package com.espertech.esper.common.internal.epl.approx.countminsketch;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class CountMinSketchSpecHashes {
    public final static EPTypeClass EPTYPE = new EPTypeClass(CountMinSketchSpecHashes.class);

    private double epsOfTotalCount;
    private double confidence;
    private int seed;

    public CountMinSketchSpecHashes(double epsOfTotalCount, double confidence, int seed) {
        this.epsOfTotalCount = epsOfTotalCount;
        this.confidence = confidence;
        this.seed = seed;
    }

    public double getEpsOfTotalCount() {
        return epsOfTotalCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getSeed() {
        return seed;
    }

    public void setEpsOfTotalCount(double epsOfTotalCount) {
        this.epsOfTotalCount = epsOfTotalCount;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public CodegenExpression codegenMake(CodegenMethod method, CodegenClassScope classScope) {
        return newInstance(CountMinSketchSpecHashes.EPTYPE, constant(epsOfTotalCount), constant(confidence), constant(seed));
    }
}

