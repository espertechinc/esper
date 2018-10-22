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
package com.espertech.esper.common.internal.epl.agg.access.core;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReader;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationTableAccessAggReaderForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class AggregationTableAccessAggReaderCodegenField implements CodegenFieldSharable {

    private final AggregationTableAccessAggReaderForge readerForge;
    private final CodegenClassScope classScope;
    private final Class generator;

    public AggregationTableAccessAggReaderCodegenField(AggregationTableAccessAggReaderForge readerForge, CodegenClassScope classScope, Class generator) {
        this.readerForge = readerForge;
        this.classScope = classScope;
        this.generator = generator;
    }

    public Class type() {
        return AggregationMultiFunctionTableReader.class;
    }

    public CodegenExpression initCtorScoped() {
        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        CodegenMethod init = classScope.getPackageScope().getInitMethod().makeChildWithScope(AggregationMultiFunctionTableReader.class, generator, symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        init.getBlock().methodReturn(readerForge.codegenCreateReader(init, symbols, classScope));
        return localMethod(init, EPStatementInitServices.REF);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregationTableAccessAggReaderCodegenField that = (AggregationTableAccessAggReaderCodegenField) o;

        return readerForge.equals(that.readerForge);
    }

    public int hashCode() {
        return readerForge.hashCode();
    }
}
