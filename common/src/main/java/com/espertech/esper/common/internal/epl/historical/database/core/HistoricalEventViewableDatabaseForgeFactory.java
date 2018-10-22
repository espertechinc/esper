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
package com.espertech.esper.common.internal.epl.historical.database.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.client.hook.type.SQLColumnTypeContext;
import com.espertech.esper.common.client.hook.type.SQLColumnTypeConversion;
import com.espertech.esper.common.client.hook.type.SQLOutputRowConversion;
import com.espertech.esper.common.client.hook.type.SQLOutputRowTypeContext;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage1.spec.DBStatementStreamSpec;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigException;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConnectionFactory;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.function.Function;

public class HistoricalEventViewableDatabaseForgeFactory {
    private static final Logger log = LoggerFactory.getLogger(HistoricalEventViewableDatabaseForgeFactory.class);

    /**
     * Placeholder name for SQL-where clause substitution.
     */
    public static final String SAMPLE_WHERECLAUSE_PLACEHOLDER = "$ESPER-SAMPLE-WHERE";

    public static HistoricalEventViewableDatabaseForge createDBStatementView(int streamNum, DBStatementStreamSpec sqlStreamSpec, SQLColumnTypeConversion columnTypeConversionHook, SQLOutputRowConversion outputRowConversionHook, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {

        // Parse the SQL for placeholders and text fragments
        List<PlaceholderParser.Fragment> sqlFragments;
        try {
            sqlFragments = PlaceholderParser.parsePlaceholder(sqlStreamSpec.getSqlWithSubsParams());
        } catch (PlaceholderParseException ex) {
            String text = "Error parsing SQL";
            throw new ExprValidationException(text + ", reason: " + ex.getMessage());
        }

        // Assemble a PreparedStatement and parameter list
        String preparedStatementText = createPreparedStatement(sqlFragments);
        SQLParameterDesc parameterDesc = getParameters(sqlFragments);
        if (log.isDebugEnabled()) {
            log.debug(".createDBEventStream preparedStatementText=" + preparedStatementText +
                    " parameterDesc=" + parameterDesc);
        }

        // Get a database connection
        String databaseName = sqlStreamSpec.getDatabaseName();
        DatabaseConnectionFactory databaseConnectionFactory;
        ColumnSettings metadataSetting;
        try {
            databaseConnectionFactory = services.getDatabaseConfigServiceCompileTime().getConnectionFactory(databaseName);
            metadataSetting = services.getDatabaseConfigServiceCompileTime().getQuerySetting(databaseName);
        } catch (Exception ex) {
            String text = "Error connecting to database '" + databaseName + '\'';
            log.error(text, ex);
            throw new ExprValidationException(text + ", reason: " + ex.getMessage(), ex);
        }

        Connection connection;
        try {
            connection = databaseConnectionFactory.getConnection();
        } catch (DatabaseConfigException ex) {
            String text = "Error connecting to database '" + databaseName + '\'';
            log.error(text, ex);
            throw new ExprValidationException(text + ", reason: " + ex.getMessage(), ex);
        }

        // On default setting, if we detect Oracle in the connection then don't query metadata from prepared statement
        ConfigurationCommonDBRef.MetadataOriginEnum metaOriginPolicy = metadataSetting.getMetadataRetrievalEnum();
        if (metaOriginPolicy == ConfigurationCommonDBRef.MetadataOriginEnum.DEFAULT) {
            String connectionClass = connection.getClass().getName();
            if (connectionClass.toLowerCase(Locale.ENGLISH).contains("oracle") || (connectionClass.toLowerCase(Locale.ENGLISH).contains("timesten"))) {
                // switch to sample statement if we are dealing with an oracle connection
                metaOriginPolicy = ConfigurationCommonDBRef.MetadataOriginEnum.SAMPLE;
            }
        }

        QueryMetaData queryMetaData;
        try {
            if ((metaOriginPolicy == ConfigurationCommonDBRef.MetadataOriginEnum.METADATA) || (metaOriginPolicy == ConfigurationCommonDBRef.MetadataOriginEnum.DEFAULT)) {
                queryMetaData = getPreparedStmtMetadata(connection, parameterDesc.getParameters(), preparedStatementText, metadataSetting);
            } else {
                String sampleSQL;
                boolean isGivenMetadataSQL = true;
                if (sqlStreamSpec.getMetadataSQL() != null) {
                    sampleSQL = sqlStreamSpec.getMetadataSQL();
                    isGivenMetadataSQL = true;
                    if (log.isInfoEnabled()) {
                        log.info(".createDBStatementView Using provided sample SQL '" + sampleSQL + "'");
                    }
                } else {
                    // Create the sample SQL by replacing placeholders with null and
                    // SAMPLE_WHERECLAUSE_PLACEHOLDER with a "where 1=0" clause
                    sampleSQL = createSamplePlaceholderStatement(sqlFragments);

                    if (log.isInfoEnabled()) {
                        log.info(".createDBStatementView Using un-lexed sample SQL '" + sampleSQL + "'");
                    }

                    // If there is no SAMPLE_WHERECLAUSE_PLACEHOLDER, lexical analyse the SQL
                    // adding a "where 1=0" clause.
                    if (parameterDesc.getBuiltinIdentifiers().length != 1) {
                        sampleSQL = services.getCompilerServices().lexSampleSQL(sampleSQL);
                        if (log.isInfoEnabled()) {
                            log.info(".createDBStatementView Using lexed sample SQL '" + sampleSQL + "'");
                        }
                    }
                }

                // finally get the metadata by firing the sample SQL
                queryMetaData = getExampleQueryMetaData(connection, parameterDesc.getParameters(), sampleSQL, metadataSetting, isGivenMetadataSQL);
            }
        } catch (ExprValidationException ex) {
            try {
                connection.close();
            } catch (SQLException e) {
                // don't handle
            }
            throw ex;
        }

        // Close connection
        try {
            connection.close();
        } catch (SQLException e) {
            String text = "Error closing connection";
            log.error(text, e);
            throw new ExprValidationException(text + ", reason: " + e.getMessage(), e);
        }

        // Create event type
        // Construct an event type from SQL query result metadata
        Map<String, Object> eventTypeFields = new HashMap<String, Object>();
        int columnNum = 1;
        for (Map.Entry<String, DBOutputTypeDesc> entry : queryMetaData.getOutputParameters().entrySet()) {
            String name = entry.getKey();
            DBOutputTypeDesc dbOutputDesc = entry.getValue();

            Class clazz;
            if (dbOutputDesc.getOptionalBinding() != null) {
                clazz = dbOutputDesc.getOptionalBinding().getType();
            } else {
                clazz = SQLTypeMapUtil.sqlTypeToClass(dbOutputDesc.getSqlType(), dbOutputDesc.getClassName(), services.getClasspathImportServiceCompileTime().getClassForNameProvider());
            }

            if (columnTypeConversionHook != null) {

                Class newValue = columnTypeConversionHook.getColumnType(new SQLColumnTypeContext(sqlStreamSpec.getDatabaseName(), sqlStreamSpec.getSqlWithSubsParams(), name, clazz, dbOutputDesc.getSqlType(), columnNum));
                if (newValue != null) {
                    clazz = newValue;
                }

            }
            eventTypeFields.put(name, clazz);
            columnNum++;
        }

        EventType eventType;
        String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousDBHistorical(streamNum);
        Function<EventTypeApplicationType, EventTypeMetadata> metadata = appType -> new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.DBDERIVED, appType, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        if (outputRowConversionHook == null) {
            eventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(EventTypeApplicationType.MAP), eventTypeFields, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        } else {
            Class carrierClass = outputRowConversionHook.getOutputRowType(new SQLOutputRowTypeContext(sqlStreamSpec.getDatabaseName(), sqlStreamSpec.getSqlWithSubsParams(), eventTypeFields));
            if (carrierClass == null) {
                throw new ExprValidationException("Output row conversion hook returned no type");
            }
            BeanEventTypeStem stem = services.getBeanEventTypeStemService().getCreateStem(carrierClass, null);
            eventType = new BeanEventType(stem, metadata.apply(EventTypeApplicationType.CLASS), services.getBeanEventTypeFactoryPrivate(), null, null, null, null);
        }
        services.getEventTypeCompileTimeRegistry().newType(eventType);

        return new HistoricalEventViewableDatabaseForge(streamNum, eventType, databaseName,
                queryMetaData.getInputParameters().toArray(new String[queryMetaData.getInputParameters().size()]),
                preparedStatementText, queryMetaData.getOutputParameters());
    }

    private static QueryMetaData getExampleQueryMetaData(Connection connection, String[] parameters, String sampleSQL, ColumnSettings metadataSetting, boolean isUsingMetadataSQL)
            throws ExprValidationException {
        // Simply add up all input parameters
        List<String> inputParameters = new LinkedList<String>();
        inputParameters.addAll(Arrays.asList(parameters));

        Statement statement;
        try {
            statement = connection.createStatement();
        } catch (SQLException ex) {
            String text = "Error creating statement";
            log.error(text, ex);
            throw new ExprValidationException(text + ", reason: " + ex.getMessage());
        }

        ResultSet result = null;
        try {
            result = statement.executeQuery(sampleSQL);
        } catch (SQLException ex) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.info("Error closing statement: " + e.getMessage(), e);
            }

            String text;
            if (isUsingMetadataSQL) {
                text = "Error compiling metadata SQL to retrieve statement metadata, using sql text '" + sampleSQL + "'";
            } else {
                text = "Error compiling metadata SQL to retrieve statement metadata, consider using the 'metadatasql' syntax, using sql text '" + sampleSQL + "'";
            }

            log.error(text, ex);
            throw new ExprValidationException(text + ", reason: " + ex.getMessage());
        }

        Map<String, DBOutputTypeDesc> outputProperties;
        try {
            outputProperties = compileResultMetaData(result.getMetaData(), metadataSetting);
        } catch (SQLException ex) {
            try {
                result.close();
            } catch (SQLException e) {
                // don't handle
            }
            try {
                statement.close();
            } catch (SQLException e) {
                // don't handle
            }
            String text = "Error in statement '" + sampleSQL + "', failed to obtain result metadata";
            log.error(text, ex);
            throw new ExprValidationException(text + ", please check the statement, reason: " + ex.getMessage());
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    log.warn("Exception closing result set: " + e.getMessage());
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    log.warn("Exception closing result set: " + e.getMessage());
                }
            }
        }

        return new QueryMetaData(inputParameters, outputProperties);
    }

    private static QueryMetaData getPreparedStmtMetadata(Connection connection,
                                                         String[] parameters,
                                                         String preparedStatementText,
                                                         ColumnSettings metadataSetting)
            throws ExprValidationException {
        PreparedStatement prepared;
        try {
            if (log.isInfoEnabled()) {
                log.info(".getPreparedStmtMetadata Preparing statement '" + preparedStatementText + "'");
            }
            prepared = connection.prepareStatement(preparedStatementText);
        } catch (SQLException ex) {
            String text = "Error preparing statement '" + preparedStatementText + '\'';
            log.error(text, ex);
            throw new ExprValidationException(text + ", reason: " + ex.getMessage());
        }

        // Interrogate prepared statement - parameters and result
        List<String> inputParameters = new LinkedList<String>();
        try {
            ParameterMetaData parameterMetaData = prepared.getParameterMetaData();
            inputParameters.addAll(Arrays.asList(parameters).subList(0, parameterMetaData.getParameterCount()));
        } catch (Exception ex) {
            try {
                prepared.close();
            } catch (SQLException e) {
                // don't handle
            }
            String text = "Error obtaining parameter metadata from prepared statement, consider turning off metadata interrogation via configuration, for statement '" + preparedStatementText + '\'';
            log.error(text, ex);
            throw new ExprValidationException(text + ", please check the statement, reason: " + ex.getMessage());
        }

        Map<String, DBOutputTypeDesc> outputProperties;
        try {
            outputProperties = compileResultMetaData(prepared.getMetaData(), metadataSetting);
        } catch (SQLException ex) {
            try {
                prepared.close();
            } catch (SQLException e) {
                // don't handle
            }
            String text = "Error in statement '" + preparedStatementText + "', failed to obtain result metadata, consider turning off metadata interrogation via configuration";
            log.error(text, ex);
            throw new ExprValidationException(text + ", please check the statement, reason: " + ex.getMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug(".createDBEventStream in=" + inputParameters.toString() +
                    " out=" + outputProperties.toString());
        }

        // Close statement
        try {
            prepared.close();
        } catch (SQLException e) {
            String text = "Error closing prepared statement";
            log.error(text, e);
            throw new ExprValidationException(text + ", reason: " + e.getMessage());
        }

        return new QueryMetaData(inputParameters, outputProperties);
    }

    private static String createPreparedStatement(List<PlaceholderParser.Fragment> parseFragements) {
        StringBuilder buffer = new StringBuilder();
        for (PlaceholderParser.Fragment fragment : parseFragements) {
            if (!fragment.isParameter()) {
                buffer.append(fragment.getValue());
            } else {
                if (fragment.getValue().equals(SAMPLE_WHERECLAUSE_PLACEHOLDER)) {
                    continue;
                }
                buffer.append('?');
            }
        }
        return buffer.toString();
    }

    private static String createSamplePlaceholderStatement(List<PlaceholderParser.Fragment> parseFragements) {
        StringBuilder buffer = new StringBuilder();
        for (PlaceholderParser.Fragment fragment : parseFragements) {
            if (!fragment.isParameter()) {
                buffer.append(fragment.getValue());
            } else {
                if (fragment.getValue().equals(SAMPLE_WHERECLAUSE_PLACEHOLDER)) {
                    buffer.append(" where 1=0 ");
                    break;
                } else {
                    buffer.append("null");
                }
            }
        }
        return buffer.toString();
    }

    private static SQLParameterDesc getParameters(List<PlaceholderParser.Fragment> parseFragements) {
        List<String> eventPropertyParams = new LinkedList<String>();
        for (PlaceholderParser.Fragment fragment : parseFragements) {
            if (fragment.isParameter()) {
                if (!fragment.getValue().equals(SAMPLE_WHERECLAUSE_PLACEHOLDER)) {
                    eventPropertyParams.add(fragment.getValue());
                }
            }
        }
        String[] parameters = eventPropertyParams.toArray(new String[eventPropertyParams.size()]);
        String[] builtin = eventPropertyParams.toArray(new String[eventPropertyParams.size()]);
        return new SQLParameterDesc(parameters, builtin);
    }

    private static Map<String, DBOutputTypeDesc> compileResultMetaData(ResultSetMetaData resultMetaData,
                                                                       ColumnSettings columnSettings
    )
            throws SQLException {
        Map<String, DBOutputTypeDesc> outputProperties = new HashMap<String, DBOutputTypeDesc>();
        for (int i = 0; i < resultMetaData.getColumnCount(); i++) {
            String columnName = resultMetaData.getColumnLabel(i + 1);
            if (columnName == null) {
                columnName = resultMetaData.getColumnName(i + 1);
            }
            int columnType = resultMetaData.getColumnType(i + 1);
            String javaClass = resultMetaData.getColumnTypeName(i + 1);

            ConfigurationCommonDBRef.ColumnChangeCaseEnum caseEnum = columnSettings.getColumnCaseConversionEnum();
            if ((caseEnum != null) && (caseEnum == ConfigurationCommonDBRef.ColumnChangeCaseEnum.LOWERCASE)) {
                columnName = columnName.toLowerCase(Locale.ENGLISH);
            }
            if ((caseEnum != null) && (caseEnum == ConfigurationCommonDBRef.ColumnChangeCaseEnum.UPPERCASE)) {
                columnName = columnName.toUpperCase(Locale.ENGLISH);
            }

            DatabaseTypeBinding binding = null;
            String javaTypeBinding = null;
            if (columnSettings.getJavaSqlTypeBinding() != null) {
                javaTypeBinding = columnSettings.getJavaSqlTypeBinding().get(columnType);
            }
            if (javaTypeBinding != null) {
                binding = DatabaseTypeEnum.getEnum(javaTypeBinding).getBinding();
            }
            DBOutputTypeDesc outputType = new DBOutputTypeDesc(columnType, javaClass, binding);
            outputProperties.put(columnName, outputType);
        }
        return outputProperties;
    }
}
