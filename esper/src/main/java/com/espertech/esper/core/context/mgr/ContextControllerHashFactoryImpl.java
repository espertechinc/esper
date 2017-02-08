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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionIdentifierHash;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.stmt.*;
import com.espertech.esper.epl.core.EngineImportSingleRowDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ContextDetail;
import com.espertech.esper.epl.spec.ContextDetailHash;
import com.espertech.esper.epl.spec.ContextDetailHashItem;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.epl.spec.util.StatementSpecCompiledAnalyzer;
import com.espertech.esper.epl.spec.util.StatementSpecCompiledAnalyzerResult;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.filter.*;

import java.io.StringWriter;
import java.util.*;

public class ContextControllerHashFactoryImpl extends ContextControllerHashFactoryBase implements ContextControllerFactory {

    private final ContextStatePathValueBinding binding;

    public ContextControllerHashFactoryImpl(ContextControllerFactoryContext factoryContext, ContextDetailHash hashedSpec, List<FilterSpecCompiled> filtersSpecsNestedContexts) {
        super(factoryContext, hashedSpec, filtersSpecsNestedContexts);
        this.binding = factoryContext.getStateCache().getBinding(Integer.class);
    }

    public ContextController createNoCallback(int pathId, ContextControllerLifecycleCallback callback) {
        return new ContextControllerHash(pathId, callback, this);
    }

    public ContextStatePathValueBinding getBinding() {
        return binding;
    }
}
