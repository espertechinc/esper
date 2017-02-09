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
package com.espertech.esper.client;

import com.espertech.esper.util.DatabaseTypeEnum;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Container for database configuration information, such as
 * options around getting a database connection and options to control the lifecycle
 * of connections and set connection parameters.
 */
public class ConfigurationDBRef implements Serializable {
    private static final long serialVersionUID = 2833881768128847540L;
    private ConnectionFactoryDesc connectionFactoryDesc;
    private ConnectionSettings connectionSettings;
    private ConnectionLifecycleEnum connectionLifecycleEnum;
    private ConfigurationDataCache dataCacheDesc;
    private MetadataOriginEnum metadataOrigin;
    private ColumnChangeCaseEnum columnChangeCase;
    private Map<Integer, String> sqlTypesMapping;

    /**
     * Ctor.
     */
    public ConfigurationDBRef() {
        connectionLifecycleEnum = ConnectionLifecycleEnum.RETAIN;
        connectionSettings = new ConnectionSettings();
        metadataOrigin = MetadataOriginEnum.DEFAULT;
        columnChangeCase = ColumnChangeCaseEnum.NONE;
        sqlTypesMapping = new HashMap<Integer, String>();
    }

    /**
     * Set the connection factory to use a factory class that provides an instance of {@link javax.sql.DataSource}.
     * <p>
     * This method is designed for use with Apache Commons DBCP and its BasicDataSourceFactory
     * but can also work for any application-provided factory for DataSource instances.
     * <p>
     * When using Apache DBCP, specify BasicDataSourceFactory.class.getName() as the class name
     * and populate all properties that Apache DBCP takes for connection pool configuration.
     * <p>
     * When using an application-provided data source factory, pass the class name of
     * a class that provides a public static method createDataSource(Properties properties) returning DataSource.
     *
     * @param dataSourceFactoryClassName the classname of the data source factory
     * @param properties                 passed to the createDataSource method of the data source factory class
     */
    public void setDataSourceFactory(Properties properties, String dataSourceFactoryClassName) {
        connectionFactoryDesc = new DataSourceFactory(properties, dataSourceFactoryClassName);
    }

    /**
     * Sets the connection factory to use {@link javax.sql.DataSource} to obtain a
     * connection.
     *
     * @param contextLookupName is the object name to look up via {@link javax.naming.InitialContext}
     * @param environmentProps  are the optional properties to pass to the context
     */
    public void setDataSourceConnection(String contextLookupName, Properties environmentProps) {
        connectionFactoryDesc = new DataSourceConnection(contextLookupName, environmentProps);
    }

    /**
     * Sets the connection factory to use {@link java.sql.DriverManager} to obtain a
     * connection.
     *
     * @param className      is the driver class name
     * @param url            is the URL
     * @param connectionArgs are optional connection arguments
     */
    public void setDriverManagerConnection(String className, String url, Properties connectionArgs) {
        connectionFactoryDesc = new DriverManagerConnection(className, url, connectionArgs);
    }

    /**
     * Sets the connection factory to use {@link java.sql.DriverManager} to obtain a
     * connection.
     *
     * @param className is the driver class name
     * @param url       is the URL
     * @param username  is the username to obtain a connection
     * @param password  is the password to obtain a connection
     */
    public void setDriverManagerConnection(String className, String url, String username, String password) {
        connectionFactoryDesc = new DriverManagerConnection(className, url, username, password);
    }

    /**
     * Sets the connection factory to use {@link java.sql.DriverManager} to obtain a
     * connection.
     *
     * @param className      is the driver class name
     * @param url            is the URL
     * @param username       is the username to obtain a connection
     * @param password       is the password to obtain a connection
     * @param connectionArgs are optional connection arguments
     */
    public void setDriverManagerConnection(String className, String url, String username, String password, Properties connectionArgs) {
        connectionFactoryDesc = new DriverManagerConnection(className, url, username, password, connectionArgs);
    }

    /**
     * Sets the auto-commit connection settings for new connections to this database.
     *
     * @param value is true to set auto-commit to true, or false to set auto-commit to false, or null to accepts the default
     */
    public void setConnectionAutoCommit(boolean value) {
        this.connectionSettings.setAutoCommit(value);
    }

    /**
     * Sets the transaction isolation level on new connections created for this database.
     *
     * @param value is the transaction isolation level
     */
    public void setConnectionTransactionIsolation(int value) {
        this.connectionSettings.setTransactionIsolation(value);
    }

    /**
     * Sets the read-only flag on new connections created for this database.
     *
     * @param isReadOnly is the read-only flag
     */
    public void setConnectionReadOnly(boolean isReadOnly) {
        this.connectionSettings.setReadOnly(isReadOnly);
    }

    /**
     * Sets the catalog name for new connections created for this database.
     *
     * @param catalog is the catalog name
     */
    public void setConnectionCatalog(String catalog) {
        this.connectionSettings.setCatalog(catalog);
    }

    /**
     * Returns the connection settings for this database.
     *
     * @return connection settings
     */
    public ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    /**
     * Returns the setting to control whether a new connection is obtained for each lookup,
     * or connections are retained between lookups.
     *
     * @return enum controlling connection allocation
     */
    public ConnectionLifecycleEnum getConnectionLifecycleEnum() {
        return connectionLifecycleEnum;
    }

    /**
     * Controls whether a new connection is obtained for each lookup,
     * or connections are retained between lookups.
     *
     * @param connectionLifecycleEnum is an enum controlling connection allocation
     */
    public void setConnectionLifecycleEnum(ConnectionLifecycleEnum connectionLifecycleEnum) {
        this.connectionLifecycleEnum = connectionLifecycleEnum;
    }

    /**
     * Returns the descriptor controlling connection creation settings.
     *
     * @return connection factory settings
     */
    public ConnectionFactoryDesc getConnectionFactoryDesc() {
        return connectionFactoryDesc;
    }

    /**
     * Configures a LRU cache of the given size for the database.
     *
     * @param size is the maximum number of entries before query results are evicted
     */
    public void setLRUCache(int size) {
        dataCacheDesc = new ConfigurationLRUCache(size);
    }

    /**
     * Configures an expiry-time cache of the given maximum age in seconds and purge interval in seconds.
     * <p>
     * Specifies the cache reference type to be weak references. Weak reference cache entries become
     * eligible for garbage collection and are removed from cache when the garbage collection requires so.
     *
     * @param maxAgeSeconds        is the maximum number of seconds before a query result is considered stale (also known as time-to-live)
     * @param purgeIntervalSeconds is the interval at which the engine purges stale data from the cache
     */
    public void setExpiryTimeCache(double maxAgeSeconds, double purgeIntervalSeconds) {
        dataCacheDesc = new ConfigurationExpiryTimeCache(maxAgeSeconds, purgeIntervalSeconds, ConfigurationCacheReferenceType.getDefault());
    }

    /**
     * Configures an expiry-time cache of the given maximum age in seconds and purge interval in seconds. Also allows
     * setting the reference type indicating whether garbage collection may remove entries from cache.
     *
     * @param maxAgeSeconds        is the maximum number of seconds before a query result is considered stale (also known as time-to-live)
     * @param purgeIntervalSeconds is the interval at which the engine purges stale data from the cache
     * @param cacheReferenceType   specifies the reference type to use
     */
    public void setExpiryTimeCache(double maxAgeSeconds, double purgeIntervalSeconds, ConfigurationCacheReferenceType cacheReferenceType) {
        dataCacheDesc = new ConfigurationExpiryTimeCache(maxAgeSeconds, purgeIntervalSeconds, cacheReferenceType);
    }

    /**
     * Return a query result data cache descriptor.
     *
     * @return cache descriptor
     */
    public ConfigurationDataCache getDataCacheDesc() {
        return dataCacheDesc;
    }

    /**
     * Returns an enumeration indicating how the engine retrieves metadata about the columns
     * that a given SQL query returns.
     * <p>
     * The engine requires to retrieve result column names and types in order to build a resulting
     * event type and perform expression type checking.
     *
     * @return indication how to retrieve metadata
     */
    public MetadataOriginEnum getMetadataRetrievalEnum() {
        return metadataOrigin;
    }

    /**
     * Sets and indicator how the engine should retrieve metadata about the columns
     * that a given SQL query returns.
     * <p>
     * The engine requires to retrieve result column names and types in order to build a resulting
     * event type and perform expression type checking.
     *
     * @param metadataOrigin indication how to retrieve metadata
     */
    public void setMetadataOrigin(MetadataOriginEnum metadataOrigin) {
        this.metadataOrigin = metadataOrigin;
    }

    /**
     * Returns enum value determining how the engine changes case on output column names
     * returned from statement or statement result set metadata.
     *
     * @return change case enums
     */
    public ColumnChangeCaseEnum getColumnChangeCase() {
        return columnChangeCase;
    }

    /**
     * Sets enum value determining how the engine should change case on output column names
     * returned from statement or statement result set metadata.
     *
     * @param columnChangeCaseEnum change case enums
     */
    public void setColumnChangeCase(ColumnChangeCaseEnum columnChangeCaseEnum) {
        this.columnChangeCase = columnChangeCaseEnum;
    }

    /**
     * Adds a mapping of a java.sql.Types type to a Java type.
     * <p>
     * The mapping dictates to the engine how the output column should be
     * represented as a Java Object.
     * <p>
     * Accepts a Java classname (fully-qualified or simple) or primitive type name
     * for the Java type parameter. See {@link DatabaseTypeEnum} for valid values for the java type name.
     *
     * @param sqlType      is a java.sql.Types constant, for which output columns are converted to java type
     * @param javaTypeName is a Java class name
     */
    public void addSqlTypesBinding(int sqlType, String javaTypeName) {
        DatabaseTypeEnum typeEnum = DatabaseTypeEnum.getEnum(javaTypeName);
        if (typeEnum == null) {
            String supported = Arrays.toString(DatabaseTypeEnum.values());
            throw new ConfigurationException("Unsupported java type '" + javaTypeName + "' when expecting any of: " + supported);
        }
        this.sqlTypesMapping.put(sqlType, javaTypeName);
    }

    /**
     * Adds a mapping of a java.sql.Types type to a Java type.
     * <p>
     * The mapping dictates to the engine how the output column should be
     * represented as a Java Object.
     * <p>
     * Accepts a Java class for the Java type parameter. See {@link DatabaseTypeEnum} for valid values.
     *
     * @param sqlType      is a java.sql.Types constant, for which output columns are converted to java type
     * @param javaTypeName is a Java class
     */
    public void addSqlTypesBinding(int sqlType, Class javaTypeName) {
        addSqlTypesBinding(sqlType, javaTypeName.getName());
    }

    /**
     * Returns the mapping of types that the engine must perform
     * when receiving output columns of that sql types.
     *
     * @return map of {@link java.sql.Types} types to Java types
     */
    public Map<Integer, String> getSqlTypesMapping() {
        return sqlTypesMapping;
    }

    /**
     * Supplies connectioon-level settings for a given database name.
     */
    public static class ConnectionSettings implements Serializable {
        private static final long serialVersionUID = 5463131581739739687L;
        private Boolean autoCommit;
        private String catalog;
        private Boolean readOnly;
        private Integer transactionIsolation;

        /**
         * Returns a boolean indicating auto-commit, or null if not set and default accepted.
         *
         * @return true for auto-commit on, false for auto-commit off, or null to accept the default
         */
        public Boolean getAutoCommit() {
            return autoCommit;
        }

        /**
         * Indicates whether to set any new connections for this database to auto-commit.
         *
         * @param autoCommit true to set connections to auto-commit, or false, or null to not set this value on a new connection
         */
        public void setAutoCommit(Boolean autoCommit) {
            this.autoCommit = autoCommit;
        }

        /**
         * Gets the name of the catalog to set on new database connections, or null for default.
         *
         * @return name of the catalog to set, or null to accept the default
         */
        public String getCatalog() {
            return catalog;
        }

        /**
         * Sets the name of the catalog on new database connections.
         *
         * @param catalog is the name of the catalog to set, or null to accept the default
         */
        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }

        /**
         * Returns a boolean indicating read-only, or null if not set and default accepted.
         *
         * @return true for read-only on, false for read-only off, or null to accept the default
         */
        public Boolean getReadOnly() {
            return readOnly;
        }

        /**
         * Indicates whether to set any new connections for this database to read-only.
         *
         * @param readOnly true to set connections to read-only, or false, or null to not set this value on a new connection
         */
        public void setReadOnly(Boolean readOnly) {
            this.readOnly = readOnly;
        }

        /**
         * Returns the connection settings for transaction isolation level.
         *
         * @return transaction isolation level
         */
        public Integer getTransactionIsolation() {
            return transactionIsolation;
        }

        /**
         * Sets the transaction isolation level for new database connections, can be null to accept the default.
         *
         * @param transactionIsolation transaction isolation level
         */
        public void setTransactionIsolation(int transactionIsolation) {
            this.transactionIsolation = transactionIsolation;
        }
    }

    /**
     * Enum controlling connection lifecycle.
     */
    public enum ConnectionLifecycleEnum {
        /**
         * Retain connection between lookups, not getting a new connection each lookup.
         */
        RETAIN,

        /**
         * Obtain a new connection each lookup closing the connection when done.
         */
        POOLED
    }

    /**
     * Marker for different connection factory settings.
     */
    public interface ConnectionFactoryDesc extends Serializable {
    }

    /**
     * Connection factory settings for using a DataSource.
     */
    public static class DataSourceConnection implements ConnectionFactoryDesc, Serializable {
        private String contextLookupName;
        private Properties envProperties;
        private static final long serialVersionUID = -3528568111362490390L;

        /**
         * Ctor.
         *
         * @param contextLookupName is the object name to look up
         * @param envProperties     are the context properties to use constructing InitialContext
         */
        public DataSourceConnection(String contextLookupName, Properties envProperties) {
            this.contextLookupName = contextLookupName;
            this.envProperties = envProperties;
        }

        /**
         * Returns the object name to look up in context.
         *
         * @return object name
         */
        public String getContextLookupName() {
            return contextLookupName;
        }

        /**
         * Returns the environment properties to use to establish the initial context.
         *
         * @return environment properties to construct the initial context
         */
        public Properties getEnvProperties() {
            return envProperties;
        }
    }

    /**
     * Connection factory settings for using a DriverManager.
     */
    public static class DriverManagerConnection implements ConnectionFactoryDesc, Serializable {
        private String className;
        private String url;
        private String optionalUserName;
        private String optionalPassword;
        private Properties optionalProperties;
        private static final long serialVersionUID = 9004336438099122203L;

        /**
         * Ctor.
         *
         * @param className          is the driver class name
         * @param url                is the database URL
         * @param optionalProperties is connection properties
         */
        public DriverManagerConnection(String className, String url, Properties optionalProperties) {
            this.className = className;
            this.url = url;
            this.optionalProperties = optionalProperties;
        }

        /**
         * Ctor.
         *
         * @param className        is the driver class name
         * @param url              is the database URL
         * @param optionalUserName is a user name for connecting
         * @param optionalPassword is a password for connecting
         */
        public DriverManagerConnection(String className, String url, String optionalUserName, String optionalPassword) {
            this.className = className;
            this.url = url;
            this.optionalUserName = optionalUserName;
            this.optionalPassword = optionalPassword;
        }

        /**
         * Ctor.
         *
         * @param className          is the driver class name
         * @param url                is the database URL
         * @param optionalUserName   is a user name for connecting
         * @param optionalPassword   is a password for connecting
         * @param optionalProperties is connection properties
         */
        public DriverManagerConnection(String className, String url, String optionalUserName, String optionalPassword, Properties optionalProperties) {
            this.className = className;
            this.url = url;
            this.optionalUserName = optionalUserName;
            this.optionalPassword = optionalPassword;
            this.optionalProperties = optionalProperties;
        }

        /**
         * Returns the driver manager class name.
         *
         * @return class name of driver manager
         */
        public String getClassName() {
            return className;
        }

        /**
         * Returns the database URL to use to obtains connections.
         *
         * @return URL
         */
        public String getUrl() {
            return url;
        }

        /**
         * Returns the user name to connect to the database, or null if none supplied,
         * since the user name can also be supplied through properties.
         *
         * @return user name or null if none supplied
         */
        public String getOptionalUserName() {
            return optionalUserName;
        }

        /**
         * Returns the password to connect to the database, or null if none supplied,
         * since the password can also be supplied through properties.
         *
         * @return password or null if none supplied
         */
        public String getOptionalPassword() {
            return optionalPassword;
        }

        /**
         * Returns the properties, if supplied, to use for obtaining a connection via driver manager.
         *
         * @return properties to obtain a driver manager connection, or null if none supplied
         */
        public Properties getOptionalProperties() {
            return optionalProperties;
        }
    }

    /**
     * Connection factory settings for using a Apache DBCP or other provider DataSource factory.
     */
    public static class DataSourceFactory implements ConnectionFactoryDesc, Serializable {
        private Properties properties;
        private String factoryClassname;
        private static final long serialVersionUID = 262880601816358807L;

        /**
         * Ctor.
         *
         * @param properties       to pass to the data source factory
         * @param factoryClassname the class name of the data source factory
         */
        public DataSourceFactory(Properties properties, String factoryClassname) {
            this.properties = properties;
            this.factoryClassname = factoryClassname;
        }

        /**
         * Returns the properties to pass to the static createDataSource method provided.
         *
         * @return properties to pass to createDataSource
         */
        public Properties getProperties() {
            return properties;
        }

        /**
         * Returns the class name of the data source factory.
         *
         * @return fully qualified class name
         */
        public String getFactoryClassname() {
            return factoryClassname;
        }

        /**
         * Adds a property.
         *
         * @param name  key
         * @param value value of property
         */
        public void addProperty(String name, String value) {
            properties.put(name, value);
        }
    }

    /**
     * Indicates how the engine retrieves metadata about a statement's output columns.
     */
    public enum MetadataOriginEnum {
        /**
         * By default, get output column metadata from the prepared statement, unless
         * an Oracle connection class is used in which case the behavior is SAMPLE.
         */
        DEFAULT,

        /**
         * Always get output column metadata from the prepared statement regardless of what driver
         * or connection is used.
         */
        METADATA,

        /**
         * Obtain output column metadata by executing a sample query statement at statement
         * compilation time. The sample statement
         * returns the same columns as the statement executed during event processing.
         * See the documentation for the generation or specication of the sample query statement.
         */
        SAMPLE
    }

    /**
     * Controls how output column names get reflected in the event type.
     */
    public enum ColumnChangeCaseEnum {
        /**
         * Leave the column names the way the database driver represents the column.
         */
        NONE,

        /**
         * Change case to lowercase on any column names returned by statement metadata.
         */
        LOWERCASE,

        /**
         * Change case to uppercase on any column names returned by statement metadata.
         */
        UPPERCASE
    }
}
