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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

/**
 * An SQL stream that polls via SQL for events via join.
 */
public class SQLStream extends Stream {
    private String databaseName;
    private String sqlWithSubsParams;
    private String optionalMetadataSQL;
    private static final long serialVersionUID = 2606529559298987982L;

    /**
     * Ctor.
     */
    public SQLStream() {
    }

    /**
     * Creates a new SQL-based stream.
     *
     * @param databaseName      is the database name to poll
     * @param sqlWithSubsParams is the SQL to use
     * @return stream
     */
    public static SQLStream create(String databaseName, String sqlWithSubsParams) {
        return new SQLStream(databaseName, sqlWithSubsParams, null, null);
    }

    /**
     * Creates a new SQL-based stream.
     *
     * @param databaseName      is the database name to poll
     * @param sqlWithSubsParams is the SQL to use
     * @param optStreamName     is the as-name of the stream
     * @return stream
     */
    public static SQLStream create(String databaseName, String sqlWithSubsParams, String optStreamName) {
        return new SQLStream(databaseName, sqlWithSubsParams, optStreamName, null);
    }

    /**
     * Creates a new SQL-based stream.
     *
     * @param databaseName        is the database name to poll
     * @param sqlWithSubsParams   is the SQL to use
     * @param optStreamName       is the as-name of the stream
     * @param optionalMetadataSQL optional SQL delivering metadata of statement
     * @return stream
     */
    public static SQLStream create(String databaseName, String sqlWithSubsParams, String optStreamName, String optionalMetadataSQL) {
        return new SQLStream(databaseName, sqlWithSubsParams, optStreamName, optionalMetadataSQL);
    }

    /**
     * Ctor.
     *
     * @param databaseName        is the database name to poll
     * @param sqlWithSubsParams   is the SQL to use
     * @param optStreamName       is the optional as-name of the stream, or null if unnamed
     * @param optionalMetadataSQL optional SQL delivering metadata of statement
     */
    public SQLStream(String databaseName, String sqlWithSubsParams, String optStreamName, String optionalMetadataSQL) {
        super(optStreamName);
        this.databaseName = databaseName;
        this.sqlWithSubsParams = sqlWithSubsParams;
        this.optionalMetadataSQL = optionalMetadataSQL;
    }

    /**
     * Returns the database name.
     *
     * @return database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Sets the database name.
     *
     * @param databaseName database name
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Returns the SQL with optional substitution parameters in the SQL.
     *
     * @return SQL
     */
    public String getSqlWithSubsParams() {
        return sqlWithSubsParams;
    }

    /**
     * Sets the SQL with optional substitution parameters in the SQL.
     *
     * @param sqlWithSubsParams SQL set set
     */
    public void setSqlWithSubsParams(String sqlWithSubsParams) {
        this.sqlWithSubsParams = sqlWithSubsParams;
    }


    /**
     * Returns the metadata SQL if any.
     *
     * @return metadata SQL
     */
    public String getOptionalMetadataSQL() {
        return optionalMetadataSQL;
    }

    /**
     * Sets metadata SQL.
     *
     * @param optionalMetadataSQL is the SQL to fire to obtain metadata from, or null if disabled
     */
    public void setOptionalMetadataSQL(String optionalMetadataSQL) {
        this.optionalMetadataSQL = optionalMetadataSQL;
    }

    public void toEPLStream(StringWriter writer, EPStatementFormatter formatter) {
        writer.write("sql:");
        writer.write(databaseName);
        writer.write("[\"");
        writer.write(sqlWithSubsParams);
        writer.write("\"]");
    }

    public void toEPLStreamType(StringWriter writer) {
        writer.write("sql:");
        writer.write(databaseName);
        writer.write("[..]");
    }

    public void toEPLStreamOptions(StringWriter writer) {
    }
}
