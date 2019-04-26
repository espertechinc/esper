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
package com.espertech.esper.common.internal.epl.agg.method.minmax;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMinMaxAggrNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class AggregationForgeFactoryMinMax extends AggregationForgeFactoryBase {
    protected final ExprMinMaxAggrNode parent;
    protected final Class type;
    protected final boolean hasDataWindows;
    protected final DataInputOutputSerdeForge serde;
    protected final DataInputOutputSerdeForge distinctSerde;
    private AggregatorMethod aggregator;

    public AggregationForgeFactoryMinMax(ExprMinMaxAggrNode parent, Class type, boolean hasDataWindows, DataInputOutputSerdeForge serde, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.type = type;
        this.hasDataWindows = hasDataWindows;
        this.serde = serde;
        this.distinctSerde = distinctSerde;
    }

    public Class getResultType() {
        return type;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        Class distinctType = !parent.isDistinct() ? null : type;
        if (!hasDataWindows) {
            aggregator = new AggregatorMinMaxEver(this, col, rowCtor, membersColumnized, classScope, distinctType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), serde);
        } else {
            aggregator = new AggregatorMinMax(this, col, rowCtor, membersColumnized, classScope, distinctType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter());
        }
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationMinMax(parent.isDistinct(), parent.isHasFilter(), parent.getChildNodes()[0].getForge().getEvaluationType(), parent.getMinMaxTypeEnum(), hasDataWindows);
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public ExprMinMaxAggrNode getParent() {
        return parent;
    }
}
