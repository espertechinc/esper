/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

public class TestRowPatternRecognitionRegex extends TestCase {

    private static final Log log = LogFactory.getLog(TestRowPatternRecognitionRegex.class);

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testRegex()
    {
        run(new SupportTestCaseHolder("a,b,c,d", "(A?) (B)? (C D)?")
                .add("a", new String[] {"a,null,null,null"})
                .add("b", new String[] {"null,b,null,null"})
                .add("x", null)
                .add("d", null)
                .add("c", null)
                .add("d,c", null)
                .add("c,d", new String[] {"null,null,c,d"})
                .add("a,c,d", new String[] {"a,null,null,null", "a,null,c,d", "null,null,c,d"})
                .add("b,c,d", new String[] {"null,b,null,null", "null,null,c,d", "null,b,c,d"})
                .add("a,b,c,d", new String[] {"a,b,null,null", "a,null,null,null", "null,b,null,null", "null,b,c,d", "a,b,c,d", "null,null,c,d"})
        );

        run(new SupportTestCaseHolder("a,b,c,d", "(A | B) (C | D)")
                .add("a", null)
                .add("c", null)
                .add("d,c", null)
                .add("a,b", null)
                .add("a,d", new String[] {"a,null,null,d"})
                .add("a,d,c", new String[] {"a,null,null,d"})
                .add("b,c", new String[] {"null,b,c,null"})
                .add("b,d", new String[] {"null,b,null,d"})
                .add("b,a,d,c", new String[] {"a,null,null,d"})
                .add("x,a,x,b,x,b,c,x", new String[] {"null,b,c,null"})
        );

        run(new SupportTestCaseHolder("a,b,c,d,e", "A ((B C)? | (D E)?)")
                .add("a", new String[] {"a,null,null,null,null"})
                .add("a,b,c", new String[] {"a,null,null,null,null", "a,b,c,null,null"})
                .add("a,d,e", new String[] {"a,null,null,null,null", "a,null,null,d,e"})
                .add("b,c", null)
                .add("x,d,e", null)
        );

        run(new SupportTestCaseHolder("a,b,c", "(A? B) | (A? C)")
                .add("a", null)
                .add("a,b", new String[] {"a,b,null", "null,b,null"})
                .add("a,c", new String[] {"a,null,c", "null,null,c"})
                .add("b", new String[] {"null,b,null"})
                .add("c", new String[] {"null,null,c"})
                .add("a,x,b", new String[] {"null,b,null"})
        );

        run(new SupportTestCaseHolder("a,b,c", "(A B? C)?")
                .add("x", null)
                .add("a", null)
                .add("a,c", new String[] {"a,null,c"})
                .add("a,b,c", new String[] {"a,b,c"})
        );

        run(new SupportTestCaseHolder("a,b,c", "(A? B C?)")
                .add("x", null)
                .add("a", null)
                .add("a,c", null)
                .add("b", new String[] {"null,b,null"})
                .add("a,b,c", new String[] {"a,b,null", "null,b,null", "a,b,c", "null,b,c"})
        );

        run(new SupportTestCaseHolder("a[0],b[0],a[1],b[1],c,d", "(A B)* C D")
                .add("c,d", new String[] {"null,null,null,null,c,d"})
                .add("a1,b1,c,d", new String[] {"a1,b1,null,null,c,d","null,null,null,null,c,d"})
                .add("a2,b2,x,c,d", new String[] {"null,null,null,null,c,d"})
                .add("a1,b1,a2,b2,c,d", new String[] {"null,null,null,null,c,d","a2,b2,null,null,c,d","a1,b1,a2,b2,c,d"})
        );

        run(new SupportTestCaseHolder("a[0],b[0],c[0],a[1],b[1],c[1],d[0],e[0],d[1],e[1]", "(A (B C))* (D E)+")
                .add("a,b,c", null)
                .add("d,e", new String[] {"null,null,null,null,null,null,d,e,null,null"})
                .add("a,b,c,d,e", new String[] {"a,b,c,null,null,null,d,e,null,null", "null,null,null,null,null,null,d,e,null,null"})
                .add("a1,b1,c1,a2,b2,c2,d,e", new String[] {"a1,b1,c1,a2,b2,c2,d,e,null,null", "a2,b2,c2,null,null,null,d,e,null,null", "null,null,null,null,null,null,d,e,null,null"})
                .add("d1,e1,d2,e2", new String[] {"null,null,null,null,null,null,d1,e1,null,null", "null,null,null,null,null,null,d2,e2,null,null", "null,null,null,null,null,null,d1,e1,d2,e2"})
        );

        run(new SupportTestCaseHolder("a[0],a[1],d[0],e[0],d[1],e[1]", "A+ (D E)+")
                .add("a,e,a,d,d,e,a,e,e,a,d,d,e,d,e", null)
                .add("a,d,e", new String[] {"a,null,d,e,null,null"})
                .add("a1,a2,d,e", new String[] {"a1,a2,d,e,null,null", "a2,null,d,e,null,null"})
                .add("a1,d1,e1,d2,e2", new String[] {"a1,null,d1,e1,null,null", "a1,null,d1,e1,d2,e2"})
        );

        run(new SupportTestCaseHolder("a,b,c,d,e,f", "(A (B | C)) | (D (E | F))")
                .add("a,e,d,b,a,f,f,d,c,a", null)
                .add("a,f,c,b,a,d,f", new String[] {"null,null,null,d,null,f"})
                .add("c,b,d,a,b,x,y", new String[] {"a,b,null,null,null,null"})
                .add("a,d,c,f,d,e,x,a,c", new String[] {"null,null,null,d,e,null","a,null,c,null,null,null"})
        );

        run(new SupportTestCaseHolder("a,b,c,d,e,f", "(A (B | C)? ) | (D? (E | F))")
                .add("a1,f1,c,b,a2,d,f2", new String[] {"a1,null,null,null,null,null", "a2,null,null,null,null,null","null,null,null,null,null,f1","null,null,null,null,null,f2","null,null,null,d,null,f2"})
                .add("d,f", new String[] {"null,null,null,d,null,f", "null,null,null,null,null,f"})
        );

        run(new SupportTestCaseHolder("a[0],a[1],b,c,d", "(A B C) | (A+ B D)")
                .add("a1,c,a2,b,d", new String[] {"a2,null,b,null,d"})
                .add("a1,b1,x,a2,b2,c1", new String[] {"a2,null,b2,c1,null"})
        );
    }

    private void run(SupportTestCaseHolder testDesc)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("select * from MyEvent.win:keepall() " +
                "match_recognize (\n" +
                "  measures ");

        String delimiter = "";
        for (String measure : testDesc.getMeasures().split(","))
        {
            buf.append(delimiter);
            buf.append(measure.toUpperCase() + ".theString as " + replaceBrackets(measure) + "val");
            delimiter = ",";
        }
        buf.append("\n all matches ");
        buf.append("\n after match skip to current row ");
        buf.append("\n pattern (" + testDesc.getPattern() + ") \n");
        buf.append("  define ");

        Set<String> defines = new HashSet<String>();
        for (String measure : testDesc.getMeasures().split(","))
        {
            defines.add(removeBrackets(measure).toUpperCase());
        }

        delimiter = "";
        for (String define : defines)
        {
            buf.append(delimiter);
            buf.append(define + " as (" + define + ".theString like '" + define.toLowerCase() + "%')");
            delimiter = ",\n";
        }
        buf.append(")");

        log.debug("Trying statement " + buf.toString());
        EPStatement stmt = epService.getEPAdministrator().createEPL(buf.toString());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (SupportTestCaseItem testcase : testDesc.getTestCases())
        {
            int count =  0;
            for (String testchar : testcase.getTestdata().split(","))
            {
                epService.getEPRuntime().sendEvent(new SupportRecogBean(testchar, count++));
            }

            EventBean[] iteratorData = EPAssertionUtil.iteratorToArray(stmt.iterator());
            compare(testcase.getTestdata(), iteratorData, testDesc.getMeasures(), testcase);

            EventBean[] listenerData = listener.getNewDataListFlattened();
            listener.reset();
            compare(testcase.getTestdata(), listenerData, testDesc.getMeasures(), testcase);

            stmt.stop();
            stmt.start();
        }
    }

    private String replaceBrackets(String indexed)
    {
        return indexed.replace("[","").replace("]","");
    }

    private String removeBrackets(String indexed)
    {
        int index = indexed.indexOf('[');
        if (index == -1)
        {
            return indexed;
        }
        return indexed.substring(0, indexed.length() - index - 2);
    }

    private void compare(String sent, EventBean[] received, String measures, SupportTestCaseItem testDesc) {

        String message = "For sent: " + sent;
        if (testDesc.getExpected() == null)
        {
            assertEquals(message, 0, received.length);
            return;
        }

        String[] receivedText = new String[received.length];
        for (int i = 0; i < received.length; i++)
        {
            StringBuilder buf = new StringBuilder();
            String delimiter = "";
            for (String measure : measures.split(","))
            {
                buf.append(delimiter);
                Object value = received[i].get(replaceBrackets(measure) + "val");
                buf.append(value);
                delimiter = ",";
            }
            receivedText[i] = buf.toString();
        }

        if (testDesc.getExpected().length != received.length)
        {
            log.info("expected: " + Arrays.toString(testDesc.getExpected()));
            log.info("received: " + Arrays.toString(receivedText));
            assertEquals(message, testDesc.getExpected().length,  received.length);
        }

        log.debug("comparing: " + message);
        EPAssertionUtil.assertEqualsAnyOrder(testDesc.getExpected(), receivedText);
    }
}