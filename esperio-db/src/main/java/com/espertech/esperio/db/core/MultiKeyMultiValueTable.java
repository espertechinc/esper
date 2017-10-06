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
package com.espertech.esperio.db.core;

import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiKeyMultiValueTable {
    private final static Logger log = LoggerFactory.getLogger(MultiKeyMultiValueTable.class);
    private final String tableName;
    private final String[] keyFieldNames;
    private final int[] keyTypes;    // java.sql.Types
    private final String[] valueFieldNames;
    private final int[] valueTypes;    // java.sql.Types
    private final StoreExceptionHandler storeExceptionHandler;

    private final String insertSQL;
    private final String updateSQL;
    private final String deleteSQL;
    private final String readSQL;

    /**
     * Ctor.
     *
     * @param tableName             table name
     * @param keyFieldNames         names of key fields
     * @param keyTypes              types of key fields
     * @param valueFieldNames       names of value fields
     * @param valueTypes            types of value fields
     * @param storeExceptionHandler handler for store exceptions
     */
    public MultiKeyMultiValueTable(String tableName, String[] keyFieldNames, int[] keyTypes, String[] valueFieldNames, int[] valueTypes, StoreExceptionHandler storeExceptionHandler) {
        this.tableName = tableName;
        this.keyFieldNames = keyFieldNames;
        this.keyTypes = keyTypes;
        this.valueFieldNames = valueFieldNames;
        this.valueTypes = valueTypes;
        this.storeExceptionHandler = storeExceptionHandler;
        if (storeExceptionHandler == null) {
            throw new IllegalArgumentException("No exception handler");
        }

        insertSQL = createInsertSQL();
        updateSQL = createUpdateSQL();
        deleteSQL = createDeleteSQL();
        readSQL = createReadSQL();
    }

    /**
     * Insert row, indicating a unique-key contraint violation via StoreExceptionDBDuplicateRow.
     *
     * @param connection db connection
     * @param keys       key values
     * @param values     column values
     * @throws StoreExceptionDBRel when the insert failed, such as duplicate row
     */
    public void insertValue(Connection connection, Object[] keys, Object[] values) {
        runInsert(connection, insertSQL, keys, values);
    }

    /**
     * Insert row, ignoring a unique-key contraint violation via StoreExceptionDBDuplicateRow.
     *
     * @param connection db connection
     * @param keys       key values
     * @param values     column values
     */
    public void insertValueIgnoreDup(Connection connection, Object[] keys, Object[] values) {
        try {
            runInsert(connection, insertSQL, keys, values);
        } catch (StoreExceptionDBDuplicateRow ex) {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Duplicate key encountered inserting row " + print(keys));
            }
        }
    }

    /**
     * Update row, returning an indicator whether the row was found (true) or not (false).
     *
     * @param connection db connection
     * @param keys       key values
     * @param values     column values
     * @return indicator whether row was found
     * @throws StoreExceptionDBRel failed operation
     */
    public boolean updateValue(Connection connection, Object[] keys, Object[] values) {
        return runUpdate(connection, updateSQL, keys, values);
    }

    /**
     * Delete all rows with the keys matching the subset of all keys, returning true if deleted or false if no row found to delete.
     *
     * @param connection db connection
     * @param keys       key values
     * @throws StoreExceptionDBRel failed operation
     */
    public void deleteValueSubkeyed(Connection connection, Object[] keys) {
        StringBuilder builder = new StringBuilder();
        builder.append("delete from ");
        builder.append(tableName);
        builder.append(" where ");
        String delimiter = "";
        for (int i = 0; i < keys.length; i++) {
            builder.append(delimiter);
            builder.append(keyFieldNames[i]);
            builder.append("=?");
            delimiter = " and ";
        }

        String query = builder.toString();
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "' keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(query);
            for (int i = 0; i < keys.length; i++) {
                statement.setObject(i + 1, keys[i]);
            }
            int rows = statement.executeUpdate();
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Deleted yielded " + rows + " rows");
            }
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Delete row, returning true if deleted or false if no row found to delete.
     *
     * @param connection db connection
     * @param keys       key values
     * @return indicator whether row was found and deleted (true) or not found (false)
     * @throws StoreExceptionDBRel failed operation
     */
    public boolean deleteValue(Connection connection, Object[] keys) {
        return runDelete(connection, deleteSQL, keys);
    }

    /**
     * Select for the row, and if found update the row else insert a new row.
     *
     * @param connection db connection
     * @param keys       key values
     * @param values     column values
     * @throws StoreExceptionDBRel failed operation
     */
    protected void selectInsertUpdateValue(Connection connection, Object[] keys, Object[] values) {
        boolean exists = isExistsKey(connection, keys);
        if (!exists) {
            insertValue(connection, keys, values);
        } else {
            updateValue(connection, keys, values);
        }
    }

    /**
     * Read value returning null if not found or the value (which can also be null).
     *
     * @param connection db connection
     * @param keys       to read
     * @return value
     * @throws StoreExceptionDBRel failed operation
     */
    public Object[] readValue(Connection connection, Object[] keys) {
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + readSQL + "' for keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(readSQL);
            for (int i = 0; i < keys.length; i++) {
                statement.setObject(i + 1, keys[i]);
            }

            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return null;
            }

            Object[] row = new Object[valueFieldNames.length];
            for (int i = 0; i < valueFieldNames.length; i++) {
                row[i] = DBUtil.getValue(rs, i + 1, valueTypes[i]);
            }
            return row;
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + readSQL + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Determine if the key exists.
     *
     * @param connection db connection
     * @param keys       key values
     * @return indicator whether row exists
     * @throws StoreExceptionDBRel failed operation
     */
    public boolean isExistsKey(Connection connection, Object[] keys) {
        StringBuilder builder = new StringBuilder();
        builder.append("select 1 from ");
        builder.append(tableName);

        builder.append(" where ");
        String delimiter = "";
        for (String keyField : keyFieldNames) {
            builder.append(delimiter);
            builder.append(keyField);
            builder.append("=?");
            delimiter = " and ";
        }

        String query = builder.toString();
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "' for keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(query);
            for (int i = 0; i < keys.length; i++) {
                statement.setObject(i + 1, keys[i]);
            }

            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return false;
            }

            return true;
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    private void runInsert(Connection connection, String query, Object[] keys, Object[] values) {
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "' for keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(query);
            int index = 1;
            for (Object key : keys) {
                statement.setObject(index, key);
                index++;
            }
            for (Object value : values) {
                statement.setObject(index, value);
                index++;
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            if ((ex.getSQLState() != null) && (ex.getSQLState().equals("23000"))) {
                throw new StoreExceptionDBDuplicateRow(message, ex);
            }
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    private boolean runUpdate(Connection connection, String query, Object[] keys, Object[] values) {
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "' for keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(query);
            int index = 1;
            for (int i = 0; i < values.length; i++) {
                statement.setObject(index, values[i]);
                index++;
            }
            for (int i = 0; i < keys.length; i++) {
                statement.setObject(index, keys[i]);
                index++;
            }
            int rows = statement.executeUpdate();
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Update yielded " + rows + " rows");
            }
            return rows != 0;
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Update row, and if not found insert row.
     *
     * @param connection db connection
     * @param keys       key values
     * @param values     column values
     * @throws StoreExceptionDBRel failed operation
     */
    public void updateInsertValue(Connection connection, Object[] keys, Object[] values) throws StoreExceptionDBRel {
        boolean updated = updateValue(connection, keys, values);
        if (!updated) {
            insertValue(connection, keys, values);
        }
    }

    private boolean runDelete(Connection connection, String query, Object[] keys) {
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "' for keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(query);
            for (int i = 0; i < keys.length; i++) {
                statement.setObject(i + 1, keys[i]);
            }
            int rows = statement.executeUpdate();
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Delete yielded " + rows + " rows");
            }

            return rows != 0;
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Read all rows in table.
     *
     * @param connection to use
     * @return object array of columns
     */
    public List<Object[]> readAll(Connection connection) {
        StringBuilder builder = new StringBuilder();
        builder.append("select ");

        String delimiter = "";
        for (String keyField : keyFieldNames) {
            builder.append(delimiter);
            builder.append(keyField);
            delimiter = ",";
        }
        for (String valueField : valueFieldNames) {
            builder.append(delimiter);
            builder.append(valueField);
            delimiter = ",";
        }
        builder.append(" from ");
        builder.append(tableName);

        String query = builder.toString();
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "'");
            }
            statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return Collections.EMPTY_LIST;
            }

            List<Object[]> result = new ArrayList<Object[]>();
            do {
                Object[] row = new Object[keyFieldNames.length + valueFieldNames.length];
                int index = 0;
                for (int i = 0; i < keyFieldNames.length; i++) {
                    row[index] = DBUtil.getValue(rs, index + 1, keyTypes[i]);
                    index++;
                }
                for (int i = 0; i < valueFieldNames.length; i++) {
                    row[index] = DBUtil.getValue(rs, index + 1, valueTypes[i]);
                    index++;
                }
                result.add(row);
            }
            while (rs.next());

            return result;
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Read all rows starting with the key values supplied, key value must start at the first and can between the 1st and last key.
     *
     * @param connection to use
     * @param keys       to use
     * @return list of objects
     */
    public List<Object[]> readAllSubkeyed(Connection connection, Object[] keys) {
        StringBuilder builder = new StringBuilder();
        builder.append("select ");

        String delimiter = "";
        for (String keyField : keyFieldNames) {
            builder.append(delimiter);
            builder.append(keyField);
            delimiter = ",";
        }
        for (String valueField : valueFieldNames) {
            builder.append(delimiter);
            builder.append(valueField);
            delimiter = ",";
        }
        builder.append(" from ");
        builder.append(tableName);
        builder.append(" where ");
        delimiter = "";
        for (int i = 0; i < keys.length; i++) {
            builder.append(delimiter);
            builder.append(keyFieldNames[i]);
            builder.append("=?");
            delimiter = " and ";
        }

        String query = builder.toString();
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing query '" + query + "' for keys '" + print(keys) + "'");
            }
            statement = connection.prepareStatement(query);
            for (int i = 0; i < keys.length; i++) {
                statement.setObject(i + 1, keys[i]);
            }

            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return Collections.EMPTY_LIST;
            }

            List<Object[]> result = new ArrayList<Object[]>();
            do {
                Object[] row = new Object[keyFieldNames.length + valueFieldNames.length];
                int index = 0;
                for (int i = 0; i < keyFieldNames.length; i++) {
                    row[index] = DBUtil.getValue(rs, index + 1, keyTypes[i]);
                    index++;
                }
                for (int i = 0; i < valueFieldNames.length; i++) {
                    row[index] = DBUtil.getValue(rs, index + 1, valueTypes[i]);
                    index++;
                }
                result.add(row);
            }
            while (rs.next());

            return result;
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + query + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }

    private String print(Object[] keys) {
        return Arrays.toString(keys);
    }

    private String createInsertSQL() {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into ");
        builder.append(tableName);
        builder.append("(");
        String delimiter = "";
        for (String key : keyFieldNames) {
            builder.append(delimiter);
            builder.append(key);
            delimiter = ",";
        }
        for (String key : valueFieldNames) {
            builder.append(delimiter);
            builder.append(key);
            delimiter = ",";
        }

        delimiter = "";
        builder.append(") values (");
        for (int i = 0; i < keyFieldNames.length; i++) {
            builder.append(delimiter);
            builder.append('?');
            delimiter = ",";
        }
        for (int i = 0; i < valueFieldNames.length; i++) {
            builder.append(delimiter);
            builder.append('?');
            delimiter = ",";
        }
        builder.append(')');
        return builder.toString();
    }

    private String createUpdateSQL() {
        StringBuilder builder = new StringBuilder();
        builder.append("update ");
        builder.append(tableName);
        builder.append(" set ");
        String delimiter = "";
        for (String valueField : valueFieldNames) {
            builder.append(delimiter);
            builder.append(valueField);
            builder.append("=?");
            delimiter = ",";
        }

        builder.append(" where ");
        delimiter = "";
        for (String keyField : keyFieldNames) {
            builder.append(delimiter);
            builder.append(keyField);
            builder.append("=?");
            delimiter = " and ";
        }
        return builder.toString();
    }

    private String createDeleteSQL() {
        StringBuilder builder = new StringBuilder();
        builder.append("delete from ");
        builder.append(tableName);
        builder.append(" where ");
        String delimiter = "";
        for (String keyField : keyFieldNames) {
            builder.append(delimiter);
            builder.append(keyField);
            builder.append("=?");
            delimiter = " and ";
        }
        return builder.toString();
    }

    private String createReadSQL() {
        StringBuilder builder = new StringBuilder();
        builder.append("select ");

        String delimiter = "";
        for (String valueField : valueFieldNames) {
            builder.append(delimiter);
            builder.append(valueField);
            delimiter = ",";
        }
        builder.append(" from ");
        builder.append(tableName);

        builder.append(" where ");
        delimiter = "";
        for (String keyField : keyFieldNames) {
            builder.append(delimiter);
            builder.append(keyField);
            builder.append("=?");
            delimiter = " and ";
        }
        return builder.toString();
    }
}
