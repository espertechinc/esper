/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esperio.db.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Blob;

public class DBUtil
{
    private static Log log = LogFactory.getLog(DBUtil.class);

    /**
     * Returns the object value for a given column and type.
     * @param rs result set
     * @param index column index
     * @param valueType value type
     * @return object value
     * @throws java.sql.SQLException if the column could not be read
     */
    public static Object getValue(ResultSet rs, int index, int valueType) throws SQLException
    {
        if (valueType == Types.INTEGER)
        {
            return rs.getInt(index);
        }
        else if (valueType == Types.BIGINT)
        {
            return rs.getLong(index);
        }
        else if (valueType == Types.BLOB)
        {
            Blob blob = rs.getBlob(index);
            return getBlobValue(blob);
        }
        return rs.getObject(index);
    }

    private static byte[] getBlobValue(Blob blob) throws SQLException
    {
        if (blob == null)
        {
            return null;
        }

        if (blob.length() > Integer.MAX_VALUE)
        {
            log.warn("Blob truncated: value larger then Integer.MAX_VALUE bytes:" + blob.length());
            return null;
        }
        int len = (int) blob.length();
        return blob.getBytes(1, len);
    }
}
