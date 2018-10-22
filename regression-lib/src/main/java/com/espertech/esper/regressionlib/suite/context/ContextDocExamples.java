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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionSelectorCategory;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ContextDocExamples implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        create(env, path, "create context SegmentedByCustomer partition by custId from BankTxn");
        create(env, path, "context SegmentedByCustomer select custId, account, sum(amount) from BankTxn group by account");
        create(env, path, "context SegmentedByCustomer\n" +
            "select * from pattern [\n" +
            "every a=BankTxn(amount > 400) -> b=BankTxn(amount > 400) where timer:within(10 minutes)\n" +
            "]");
        undeployClearPath(env, path);
        create(env, path, "create context SegmentedByCustomer partition by\n" +
            "custId from BankTxn, loginId from LoginEvent, loginId from LogoutEvent");
        undeployClearPath(env, path);
        create(env, path, "create context SegmentedByCustomer partition by\n" +
            "custId from BankTxn, loginId from LoginEvent(failed=false)");
        undeployClearPath(env, path);
        create(env, path, "create context ByCustomerAndAccount partition by custId and account from BankTxn");
        create(env, path, "context ByCustomerAndAccount select custId, account, sum(amount) from BankTxn");
        create(env, path, "context ByCustomerAndAccount\n" +
            "  select context.name, context.id, context.key1, context.key2 from BankTxn");
        undeployClearPath(env, path);
        create(env, path, "create context ByCust partition by custId from BankTxn");
        create(env, path, "context ByCust\n" +
            "select * from BankTxn as t1 unidirectional, BankTxn#time(30) t2\n" +
            "where t1.amount = t2.amount");
        create(env, path, "context ByCust\n" +
            "select * from SecurityEvent as t1 unidirectional, BankTxn#time(30) t2\n" +
            "where t1.customerName = t2.customerName");
        undeployClearPath(env, path);
        create(env, path, "create context CategoryByTemp\n" +
            "group temp < 65 as cold,\n" +
            "group temp between 65 and 85 as normal,\n" +
            "group temp > 85 as large\n" +
            "from SensorEvent");
        create(env, path, "context CategoryByTemp select context.label, count(*) from SensorEvent");
        create(env, path, "context CategoryByTemp\n" +
            "select context.name, context.id, context.label from SensorEvent");
        create(env, path, "create context NineToFive start (0, 9, *, *, *) end (0, 17, *, *, *)");
        create(env, path, "context NineToFive select * from TrafficEvent(speed >= 100)");
        create(env, path, "context NineToFive\n" +
            "select context.name, context.startTime, context.endTime from TrafficEvent(speed >= 100)");
        create(env, path, "create context CtxTrainEnter\n" +
            "initiated by TrainEnterEvent as te\n" +
            "terminated after 5 minutes");
        create(env, path, "context CtxTrainEnter\n" +
            "select *, context.te.trainId, context.id, context.name from TrainLeaveEvent(trainId = context.te.trainId)");
        create(env, path, "context CtxTrainEnter\n" +
            "select t1 from pattern [\n" +
            "t1=TrainEnterEvent -> timer:interval(5 min) and not TrainLeaveEvent(trainId = context.te.trainId)]");
        create(env, path, "create context CtxEachMinute\n" +
            "initiated by pattern [every timer:interval(1 minute)]\n" +
            "terminated after 1 minutes");
        create(env, path, "context CtxEachMinute select avg(temp) from SensorEvent");
        create(env, path, "context CtxEachMinute\n" +
            "select context.id, avg(temp) from SensorEvent output snapshot when terminated");
        create(env, path, "context CtxEachMinute\n" +
            "select context.id, avg(temp) from SensorEvent output snapshot every 1 minute and when terminated");
        create(env, path, "select venue, ccyPair, side, sum(qty)\n" +
            "from CumulativePrice\n" +
            "where side='O'\n" +
            "group by venue, ccyPair, side");
        create(env, path, "create context MyContext partition by venue, ccyPair, side from CumulativePrice(side='O')");
        create(env, path, "context MyContext select venue, ccyPair, side, sum(qty) from CumulativePrice");

        create(env, path, "create context SegmentedByCustomerHash\n" +
            "coalesce by consistent_hash_crc32(custId) from BankTxn granularity 16 preallocate");
        create(env, path, "context SegmentedByCustomerHash\n" +
            "select custId, account, sum(amount) from BankTxn group by custId, account");
        create(env, path, "create context HashedByCustomer as coalesce\n" +
            "consistent_hash_crc32(custId) from BankTxn,\n" +
            "consistent_hash_crc32(loginId) from LoginEvent,\n" +
            "consistent_hash_crc32(loginId) from LogoutEvent\n" +
            "granularity 32 preallocate");

        undeployClearPath(env, path);
        create(env, path, "create context HashedByCustomer\n" +
            "coalesce consistent_hash_crc32(loginId) from LoginEvent(failed = false)\n" +
            "granularity 1024 preallocate");
        create(env, path, "create context ByCustomerHash coalesce consistent_hash_crc32(custId) from BankTxn granularity 1024");
        create(env, path, "context ByCustomerHash\n" +
            "select context.name, context.id from BankTxn");

        create(env, path, "create context NineToFiveSegmented\n" +
            "context NineToFive start (0, 9, *, *, *) end (0, 17, *, *, *),\n" +
            "context SegmentedByCustomer partition by custId from BankTxn");
        create(env, path, "context NineToFiveSegmented\n" +
            "select custId, account, sum(amount) from BankTxn group by account");
        create(env, path, "create context CtxNestedTrainEnter\n" +
            "context InitCtx initiated by TrainEnterEvent as te terminated after 5 minutes,\n" +
            "context HashCtx coalesce by consistent_hash_crc32(tagId) from PassengerScanEvent\n" +
            "granularity 16 preallocate");
        create(env, path, "context CtxNestedTrainEnter\n" +
            "select context.InitCtx.te.trainId, context.HashCtx.id,\n" +
            "tagId, count(*) from PassengerScanEvent group by tagId");
        create(env, path, "context NineToFiveSegmented\n" +
            "select context.NineToFive.startTime, context.SegmentedByCustomer.key1 from BankTxn");
        create(env, path, "context NineToFiveSegmented select context.name, context.id from BankTxn");

        create(env, path, "create context MyContext start MyStartEvent end MyEndEvent");
        create(env, path, "create context MyContext2 initiated MyEvent(level > 0) terminated after 10 seconds");
        create(env, path, "create context MyContext3 \n" +
            "start MyEvent as myevent\n" +
            "end MyEvent(id=myevent.id)");
        create(env, path, "create context MyContext4 \n" +
            "initiated by MyInitEvent as e1 \n" +
            "terminated by MyTermEvent(id=e1.id, level <> e1.level)");
        create(env, path, "create context MyContext5 start pattern [StartEventOne or StartEventTwo] end after 5 seconds");
        create(env, path, "create context MyContext6 initiated by pattern [every MyInitEvent -> MyOtherEvent where timer:within(5)] terminated by MyTermEvent");
        create(env, path, "create context MyContext7 \n" +
            "  start pattern [a=StartEventOne or  b=StartEventTwo]\n" +
            "  end pattern [EndEventOne(id=a.id) or EndEventTwo(id=b.id)]");
        create(env, path, "create context MyContext8 initiated (*, *, *, *, *) terminated after 10 seconds");
        create(env, path, "create context NineToFive start after 10 seconds end after 1 minute");
        create(env, path, "create context Overlap5SecFor1Min initiated after 5 seconds terminated after 1 minute");
        create(env, path, "create context CtxSample\n" +
            "initiated by MyStartEvent as startevent\n" +
            "terminated by MyEndEvent(id = startevent.id) as endevent");
        create(env, path, "context CtxSample select context.endevent.id, count(*) from MyEvent output snapshot when terminated");

        create(env, path, "create context TxnCategoryContext \n" +
            "  group by amount < 100 as small, \n" +
            "  group by amount between 100 and 1000 as medium, \n" +
            "  group by amount > 1000 as large from BankTxn");
        create(env, path, "@name('s0') context TxnCategoryContext select * from BankTxn#time(1 minute)");
        ContextPartitionSelectorCategory categorySmall = new ContextPartitionSelectorCategory() {
            public Set<String> getLabels() {
                return Collections.singleton("small");
            }
        };
        env.statement("s0").iterator(categorySmall);
        ContextPartitionSelectorCategory categorySmallMed = new ContextPartitionSelectorCategory() {
            public Set<String> getLabels() {
                return new HashSet<String>(Arrays.asList("small", "medium"));
            }
        };
        create(env, path, "context TxnCategoryContext create window BankTxnWindow#time(1 min) as BankTxn");
        EPCompiled faf = env.compileFAF("select count(*) from BankTxnWindow", path);
        env.runtime().getFireAndForgetService().executeQuery(faf, new ContextPartitionSelector[]{categorySmallMed});

        create(env, path, "create context CtxPerKeysAndExternallyControlled\n" +
            "context PartitionedByKeys " +
            "  partition by " +
            "    key1, key2 from MyTwoKeyInit\n," +
            "    key1, key2 from SensorEvent\n," +
            "context InitiateAndTerm initiated by MyTwoKeyInit as e1 terminated by MyTwoKeyTerm(key1=e1.key1 and key2=e1.key2)");
        create(env, path, "context CtxPerKeysAndExternallyControlled\n" +
            "select key1, key2, avg(temp) as avgTemp, count(*) as cnt\n" +
            "from SensorEvent\n" +
            "output snapshot when terminated\n" +
            "// note: no group-by needed since \n");

        create(env, path, "create context PerCustId_TriggeredByLargeAmount\n" +
            "  partition by custId from BankTxn \n" +
            "  initiated by BankTxn(amount>100) as largeTxn");
        create(env, path, "context PerCustId_TriggeredByLargeAmount select context.largeTxn, custId, sum(amount) from BankTxn");
        create(env, path, "create context PerCustId_UntilExpired\n" +
            "  partition by custId from BankTxn \n" +
            "  terminated by BankTxn(expired=true)");
        create(env, path, "context PerCustId_UntilExpired select custId, sum(amount) from BankTxn output last when terminated");
        create(env, path, "create context PerCustId_TriggeredByLargeAmount_UntilExpired\n" +
            "  partition by custId from BankTxn \n" +
            "  initiated by BankTxn(amount>100) as txn\n" +
            "  terminated by BankTxn(expired=true and user=txn.user)");
        create(env, path, "create context PerCust_AmountGreater100\n" +
            "  partition by custId from BankTxn(amount>100)\n" +
            "  initiated by BankTxn");
        create(env, path, "context PerCust_AmountGreater100 select custId, sum(amount) from BankTxn");
        create(env, path, "create context PerCust_TriggeredByLargeTxn\n" +
            "  partition by custId from BankTxn\n" +
            "  initiated by BankTxn(amount>100)");
        create(env, path, "context PerCust_TriggeredByLargeTxn select custId, sum(amount) from BankTxn");

        env.undeployAll();
    }

    private void undeployClearPath(RegressionEnvironment env, RegressionPath path) {
        env.undeployAll();
        path.clear();
    }

    private void create(RegressionEnvironment env, RegressionPath path, String epl) {
        env.compileDeploy(epl, path);
    }

    public static class CumulativePrice {
        private String venue;
        private String ccyPair;
        private String side;
        private double qty;

        public String getVenue() {
            return venue;
        }

        public String getCcyPair() {
            return ccyPair;
        }

        public String getSide() {
            return side;
        }

        public double getQty() {
            return qty;
        }
    }

    public static class TrainLeaveEvent {
        private int trainId;

        public int getTrainId() {
            return trainId;
        }
    }

    public static class TrainEnterEvent {
        private int trainId;

        public int getTrainId() {
            return trainId;
        }
    }

    public static class TrafficEvent {
        private double speed;

        public double getSpeed() {
            return speed;
        }
    }

    public static class SensorEvent {
        private double temp;
        private int key1;
        private int key2;

        public double getTemp() {
            return temp;
        }

        public int getKey1() {
            return key1;
        }

        public void setKey1(int key1) {
            this.key1 = key1;
        }

        public int getKey2() {
            return key2;
        }

        public void setKey2(int key2) {
            this.key2 = key2;
        }
    }

    public static class LoginEvent {
        private String loginId;
        private boolean failed;

        public String getLoginId() {
            return loginId;
        }

        public boolean isFailed() {
            return failed;
        }
    }

    public static class LogoutEvent {
        private String loginId;

        public String getLoginId() {
            return loginId;
        }
    }

    public static class SecurityEvent {
        private String customerName;

        public String getCustomerName() {
            return customerName;
        }
    }

    public static class BankTxn {
        private String custId;
        private String account;
        private long amount;
        private String customerName;
        private boolean expired;
        private String user;

        public String getCustId() {
            return custId;
        }

        public String getAccount() {
            return account;
        }

        public long getAmount() {
            return amount;
        }

        public String getCustomerName() {
            return customerName;
        }

        public boolean isExpired() {
            return expired;
        }

        public String getUser() {
            return user;
        }
    }

    public static class PassengerScanEvent {

        private final String tagId;

        public PassengerScanEvent(String tagId) {
            this.tagId = tagId;
        }

        public String getTagId() {
            return tagId;
        }
    }

    public static class MyStartEvent {
        private int id;
        private int level;

        public int getLevel() {
            return level;
        }

        public int getId() {
            return id;
        }
    }

    public static class MyEndEvent {
        private int id;
        private int level;

        public int getLevel() {
            return level;
        }

        public int getId() {
            return id;
        }
    }

    public static class MyInitEvent {
        private int id;
        private int level;

        public int getLevel() {
            return level;
        }

        public int getId() {
            return id;
        }
    }

    public static class MyTermEvent {
        private int id;
        private int level;

        public int getLevel() {
            return level;
        }

        public int getId() {
            return id;
        }
    }

    public static class MyEvent {
        private int id;
        private int level;

        public int getLevel() {
            return level;
        }

        public int getId() {
            return id;
        }
    }

    public static class MyTwoKeyInit {
        private int key1;
        private int key2;

        public int getKey1() {
            return key1;
        }

        public int getKey2() {
            return key2;
        }
    }

    public static class MyTwoKeyTerm {
        private int key1;
        private int key2;

        public int getKey1() {
            return key1;
        }

        public int getKey2() {
            return key2;
        }
    }
}
