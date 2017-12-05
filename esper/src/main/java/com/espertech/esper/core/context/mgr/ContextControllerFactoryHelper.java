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

import com.espertech.esper.core.context.util.ContextDetailUtil;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ContextDetail;
import com.espertech.esper.epl.spec.ContextDetailNested;
import com.espertech.esper.epl.spec.CreateContextDesc;
import com.espertech.esper.filterspec.FilterSpecCompiled;

import java.util.*;

public class ContextControllerFactoryHelper {

    public static ContextControllerFactory[] getFactory(ContextControllerFactoryServiceContext serviceContext, ContextStateCache contextStateCache) throws ExprValidationException {
        if (!(serviceContext.getDetail() instanceof ContextDetailNested)) {
            ContextControllerFactory factory = buildContextFactory(serviceContext, serviceContext.getContextName(), serviceContext.getDetail(), 1, 1, null, contextStateCache);
            factory.validateFactory();
            return new ContextControllerFactory[]{factory};
        }
        return buildNestedContextFactories(serviceContext, contextStateCache);
    }

    private static ContextControllerFactory[] buildNestedContextFactories(ContextControllerFactoryServiceContext serviceContext, ContextStateCache contextStateCache) throws ExprValidationException {
        ContextDetailNested nestedSpec = (ContextDetailNested) serviceContext.getDetail();
        // determine nested filter use
        Map<CreateContextDesc, List<FilterSpecCompiled>> filtersPerNestedContext = null;
        for (int i = 0; i < nestedSpec.getContexts().size(); i++) {
            CreateContextDesc contextParent = nestedSpec.getContexts().get(i);
            for (int j = i + 1; j < nestedSpec.getContexts().size(); j++) {
                CreateContextDesc contextControlled = nestedSpec.getContexts().get(j);
                List<FilterSpecCompiled> specs = ContextDetailUtil.getFilterSpecs(contextControlled.getContextDetail());
                if (specs == null) {
                    continue;
                }
                if (filtersPerNestedContext == null) {
                    filtersPerNestedContext = new HashMap<CreateContextDesc, List<FilterSpecCompiled>>();
                }
                List<FilterSpecCompiled> existing = filtersPerNestedContext.get(contextParent);
                if (existing != null) {
                    existing.addAll(specs);
                } else {
                    filtersPerNestedContext.put(contextParent, specs);
                }
            }
        }

        // create contexts
        Set<String> namesUsed = new HashSet<String>();
        ContextControllerFactory[] hierarchy = new ContextControllerFactory[nestedSpec.getContexts().size()];
        for (int i = 0; i < nestedSpec.getContexts().size(); i++) {
            CreateContextDesc context = nestedSpec.getContexts().get(i);

            if (namesUsed.contains(context.getContextName())) {
                throw new ExprValidationException("Context by name '" + context.getContextName() + "' has already been declared within nested context '" + serviceContext.getContextName() + "'");
            }
            namesUsed.add(context.getContextName());

            int nestingLevel = i + 1;

            List<FilterSpecCompiled> optFiltersNested = null;
            if (filtersPerNestedContext != null) {
                optFiltersNested = filtersPerNestedContext.get(context);
            }

            hierarchy[i] = buildContextFactory(serviceContext, context.getContextName(), context.getContextDetail(), nestingLevel, nestedSpec.getContexts().size(), optFiltersNested, contextStateCache);
            hierarchy[i].validateFactory();
        }
        return hierarchy;
    }

    private static ContextControllerFactory buildContextFactory(ContextControllerFactoryServiceContext serviceContext, String contextName, ContextDetail detail, int nestingLevel, int numNestingLevels, List<FilterSpecCompiled> optFiltersNested, ContextStateCache contextStateCache) throws ExprValidationException {
        ContextControllerFactoryContext factoryContext = new ContextControllerFactoryContext(serviceContext.getContextName(), contextName, serviceContext.getServicesContext(), serviceContext.getAgentInstanceContextCreate(), nestingLevel, numNestingLevels, serviceContext.isRecoveringResilient(), contextStateCache);
        return buildContextFactory(factoryContext, detail, optFiltersNested, contextStateCache);
    }

    private static ContextControllerFactory buildContextFactory(ContextControllerFactoryContext factoryContext, ContextDetail detail, List<FilterSpecCompiled> optFiltersNested, ContextStateCache contextStateCache) throws ExprValidationException {
        return factoryContext.getServicesContext().getContextControllerFactoryFactorySvc().make(factoryContext, detail, optFiltersNested);
    }
}
