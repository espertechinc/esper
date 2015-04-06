package com.espertech.esper.example.ohlc;

import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;

public class OHLCUpdateListener implements StatementAwareUpdateListener
{
    private static Log log = LogFactory.getLog(OHLCUpdateListener.class);

    public void update(EventBean[] newData, EventBean[] oldData, EPStatement epStatement, EPServiceProvider epServiceProvider)
    {
        for (int i = 0; i < newData.length; i++)
        {
            if (log.isInfoEnabled())
            {
                log.info("Statement " + String.format("%s", epStatement.getName()) + " produced: " + getProperties(newData[i]));
            }
        }
    }

    private String getProperties(EventBean theEvent)
    {
        StringBuilder buf = new StringBuilder();

        for (String name : theEvent.getEventType().getPropertyNames())
        {
            Object value = theEvent.get(name);
            buf.append(name);
            buf.append("=");

            if (name.contains("timestamp"))
            {
                buf.append(new Date((Long) value));
            }
            else
            {
                buf.append(value);
            }
            buf.append(" ");
        }
        return buf.toString();
    }
}
