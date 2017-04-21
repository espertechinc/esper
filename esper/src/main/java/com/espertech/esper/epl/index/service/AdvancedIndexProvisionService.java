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
package com.espertech.esper.epl.index.service;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.epl.index.quadtree.AdvancedIndexFactoryProviderPointRegionQuadTree;
import com.espertech.esper.epl.spec.CreateIndexItem;

import java.util.LinkedHashMap;
import java.util.Locale;

public class AdvancedIndexProvisionService {
    private final LinkedHashMap<String, AdvancedIndexFactoryProvider> providers = new LinkedHashMap<>(8);

    public AdvancedIndexProvisionService() {
        providers.put("pointregionquadtree", new AdvancedIndexFactoryProviderPointRegionQuadTree());
    }

    public AdvancedIndexProvisionDesc validateCreateIndex(String indexName, CreateIndexItem columnDesc, boolean unique, EventType eventType, StatementContext statementContext)
            throws ExprValidationException {

        // validate index type name
        String indexTypeName = columnDesc.getType().toLowerCase(Locale.ENGLISH).trim();
        AdvancedIndexFactoryProvider provider = providers.get(indexTypeName);
        if (provider == null) {
            throw new ExprValidationException("Unrecognized advanced-type index '" + indexTypeName + "'");
        }

        // validate index expressions
        ExprValidationContext validationContextColumns = getValidationContext(eventType, statementContext);
        ExprNode[] columns = columnDesc.getExpressions().toArray(new ExprNode[columnDesc.getExpressions().size()]);
        ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.CREATEINDEXCOLUMN, columns, validationContextColumns);
        ExprNodeUtility.validatePlainExpression(ExprNodeOrigin.CREATEINDEXCOLUMN, columns);

        // validate parameters
        ExprNode[] parameters = null;
        if (columnDesc.getParameters() != null && !columnDesc.getParameters().isEmpty()) {
            parameters = columnDesc.getParameters().toArray(new ExprNode[columnDesc.getParameters().size()]);
            ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.CREATEINDEXPARAMETER, parameters, validationContextColumns);
            ExprNodeUtility.validatePlainExpression(ExprNodeOrigin.CREATEINDEXPARAMETER, parameters);

            // validate no stream dependency of parameters
            ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
            for (ExprNode param : columnDesc.getParameters()) {
                param.accept(visitor);
                if (!visitor.getRefs().isEmpty()) {
                    throw new ExprValidationException("Index parameters may not refer to event properties");
                }
            }
        }

        return provider.validate(indexName, indexTypeName, unique, columns, parameters);
    }

    private ExprValidationContext getValidationContext(EventType eventType, StatementContext statementContext) {
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventType, null, false, statementContext.getEngineURI());
        return new ExprValidationContext(streamTypeService, statementContext.getEngineImportService(),
                statementContext.getStatementExtensionServicesContext(), null, statementContext.getTimeProvider(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false),
                statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), true, false, false, false, null, false);
    }
}
