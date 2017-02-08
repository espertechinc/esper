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
package com.espertech.esper.type;

import junit.framework.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class TestLongValue extends TestCase
{
    public void testLong()
    {
        LongValue lvp = new LongValue();

        assertTrue(lvp.getValueObject() == null);
        lvp.parse("10");
        assertTrue(lvp.getValueObject().equals(10L));
        assertTrue(lvp.getLong() == 10L);
        lvp.setLong(200);
        assertTrue(lvp.getLong() == 200L);
        assertTrue(lvp.getValueObject().equals(200L));

        try
        {
            lvp.setBoolean(false);
            assertTrue(false);
        }
        catch (Exception ex)
        {
            // Expected exception
        }

        try
        {
            lvp.setInt(20);
            assertTrue(false);
        }
        catch (Exception ex)
        {
            // Expected exception
        }

        try
        {
            lvp.setString("test");
            assertTrue(false);
        }
        catch (Exception ex)
        {
            // Expected exception
        }

        try
        {
            lvp = new LongValue();
            lvp.getLong();
        }
        catch (Exception ex)
        {
            // Expected exception
        }
    }

    public void testParseLong()
    {
        tryValid("0", 0);
        tryValid("11", 11);
        tryValid("12l", 12);
        tryValid("+234", 234);
        tryValid("29349349L", 29349349);
        tryValid("+29349349L", 29349349);
        tryValid("-2993L", -2993);
        tryValid("-1l", -1);

        tryInvalid("-+0");
        tryInvalid("0s");
        tryInvalid("");
        tryInvalid("l");
        tryInvalid("L");
        tryInvalid(null);
    }

    private void tryValid(String strLong, long expected)
    {
        long result = LongValue.parseString(strLong);
        assertTrue(result == expected);
    }

    private void tryInvalid(String strLong)
    {
        try
        {
            LongValue.parseString(strLong);
            assertTrue(false);
        }
        catch (Exception ex)
        {
            log.debug("Expected exception caught, msg=" + ex.getMessage());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestLongValue.class);
}
