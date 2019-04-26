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
package com.espertech.esper.common.internal.epl.agg.method.count;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprCountEverNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class AggregationForgeFactoryCountEver extends AggregationForgeFactoryBase {
    protected final ExprCountEverNode parent;
    protected final boolean ignoreNulls;
    protected final Class childType;
    protected final DataInputOutputSerdeForge distinctSerde;
    private AggregatorCount aggregator;

    public AggregationForgeFactoryCountEver(ExprCountEverNode parent, boolean ignoreNulls, Class childType, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.ignoreNulls = ignoreNulls;
        this.childType = childType;
        this.distinctSerde = distinctSerde;
    }

    public Class getResultType() {
        return long.class;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        Class distinctType = !parent.isDistinct() ? null : childType;
        aggregator = new AggregatorCount(this, col, rowCtor, membersColumnized, classScope, distinctType, distinctSerde, parent.getOptionalFilter() != null, parent.getOptionalFilter(), true);
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        Class distinctType = !parent.isDistinct() ? null : parent.getChildNodes()[0].getForge().getEvaluationType();
        return new AggregationPortableValidationCount(parent.isDistinct(), parent.getOptionalFilter() != null, true, distinctType, ignoreNulls);
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }
}