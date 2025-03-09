package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerHeavyPushMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerLightPushMsg;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.ServerConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TimeTravelTest {
    @Mock
    private Server server;
    @Mock
    private DataManagerReader dataManagerReader;
    @Mock
    private ServerConnectionManager connectionManager;
    @Mock
    private ClientServerPriorityQueue que;
    private TimeTravel sync;

    private final int serverN = 2;
    private final int myServerId = 0;
    private VectorClock myVectorClock = new VectorClock(this.serverN, this.myServerId);

    @Before
    public void setUp(){
        MockitoAnnotations.openMocks(this);

        Mockito.doAnswer(invocationOnMock -> myVectorClock).when(server).getVectorClock();
        Mockito.doAnswer(invocationOnMock -> this.myServerId).when(server).getServerID();
        Mockito.doAnswer(invocationOnMock -> this.serverN).when(server).getNumberOfServers();

    }

    private int lightPushesCounter = 0;
    @Test
    public void lightPushTest(){
        // setUp
        int randMaxDelay = ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS;
        int certainDelay = ServerConfig.LIGHT_PUSH_DELAY_MILLIS;
        ServerLightPushMsg lightPushMsg = new ServerLightPushMsg(myVectorClock);

        Mockito.doAnswer(invocationOnMock -> lightPushesCounter += 1)
                .when(connectionManager).broadcastMessage(lightPushMsg);

        int nMessages = 1;
        // test to check n message sent (probabilistically since there is a random waiting time)
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);
        try {
            Thread.sleep(nMessages * (certainDelay + randMaxDelay));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(lightPushesCounter);
        assert nMessages <= lightPushesCounter;
        assert 2*nMessages >= lightPushesCounter;
    }

    @Test
    public void checkOutOfDate(){
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);
        VectorClock parallelClock = new VectorClock(this.serverN, 1);

        parallelClock.incrementSelfClock();
        this.myVectorClock.incrementSelfClock();
        //check parallel clocks behaviour
        Assert.assertTrue(this.sync.checkOutOfDate(parallelClock));

        this.myVectorClock.updateClock(parallelClock);

        //check clock up-to-date behaviour
        Assert.assertFalse(this.sync.checkOutOfDate(parallelClock));

        parallelClock.incrementSelfClock();
        parallelClock.incrementSelfClock();
        parallelClock.updateClock(this.myVectorClock);

        //check clock remained back behaviour
        Assert.assertTrue(this.sync.checkOutOfDate(parallelClock));
    }

    @Test
    public void computeFetchDataParallelClocks(){
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);

        int increment = 5;
        VectorClock externalVector = new VectorClock(this.serverN, 1);
        List<ClockedData> myData = new LinkedList<>();
        Key lastKey = new StringKey("");

        for (int i = 0; i<increment; i++){
            this.myVectorClock.incrementSelfClock();
            lastKey = new StringKey(Integer.toString(i));
            myData.add(new ClockedData(new VectorClock(this.myVectorClock, 0), lastKey, i));

            externalVector.incrementSelfClock();
        }

        TreeMap<VectorClock, Key> myPrimaryIndex = new TreeMap<>();
        myPrimaryIndex.put(this.myVectorClock, lastKey);

        Mockito.doAnswer(invocationOnMock -> myPrimaryIndex).when(server).getSecondaryIndex();
        Mockito.doAnswer(new Answer() {
            @Override
            public List<ClockedData> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return myData;
            }
        }).when(dataManagerReader).recoverData(this.myVectorClock);

        List<ClockedData> obtainedData = this.sync.computeFetch(externalVector);
        Assert.assertEquals(myData, obtainedData);
    }

    //other server is [0,0] thus completely
    @Test
    public void computeFetchDataOtherCompletelyOutOfDate(){
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);

        int increment = 5;
        VectorClock externalVector = new VectorClock(this.serverN, 1);
        List<ClockedData> myData = new LinkedList<>();
        Key lastKey = new StringKey("");

        for (int i = 0; i<increment; i++){
            this.myVectorClock.incrementSelfClock();
            lastKey = new StringKey(Integer.toString(i));
            myData.add(new ClockedData(new VectorClock(this.myVectorClock, 0), lastKey, i));
        }

        TreeMap<VectorClock, Key> myPrimaryIndex = new TreeMap<>();
        myPrimaryIndex.put(this.myVectorClock, lastKey);

        Mockito.doAnswer(invocationOnMock -> myPrimaryIndex).when(server).getSecondaryIndex();
        Mockito.doAnswer(new Answer() {
            @Override
            public List<ClockedData> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return myData;
            }
        }).when(dataManagerReader).recoverData(this.myVectorClock);

        List<ClockedData> obtainedData = this.sync.computeFetch(externalVector);
        Assert.assertEquals(myData, obtainedData);
    }

    @Test
    public void computeFetchDataOtherPartiallyOutOfDate(){
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);

        int increment = 5;
        VectorClock externalClock = new VectorClock(this.serverN, 1);
        List<ClockedData> myData = new LinkedList<>();
        Key lastKey = new StringKey("");

        VectorClock midClock = new VectorClock(this.serverN, this.myServerId);
        List<ClockedData> myMidData = new LinkedList<>();
        Key midKey = new StringKey("");


        for (int i = 0; i<increment; i++){
            Key iterationKey = new StringKey(Integer.toString(i));
            this.myVectorClock.incrementSelfClock();
            ClockedData newData = new ClockedData(new VectorClock(this.myVectorClock, 0), lastKey, i);

            lastKey = iterationKey;
            myData.add(newData);

            if (i < increment/2){
                externalClock.updateClock(this.myVectorClock);
                midClock.updateClock(this.myVectorClock);
                myMidData.add(newData);
                midKey = iterationKey;
            }else {
                //externalClock.incrementSelfClock();
            }
        }

        TreeMap<VectorClock, Key> myPrimaryIndex = new TreeMap<>();
        myPrimaryIndex.put(this.myVectorClock, lastKey);
        myPrimaryIndex.put(midClock, midKey);

        Mockito.doAnswer(invocationOnMock -> myPrimaryIndex).when(server).getSecondaryIndex();
        Mockito.doAnswer(new Answer() {
            @Override
            public List<ClockedData> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return myData;
            }
        }).when(dataManagerReader).recoverData(new VectorClock(this.serverN, 0));
        Mockito.doAnswer(new Answer() {
            @Override
            public List<ClockedData> answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<ClockedData> copyData = new LinkedList<>(myData);
                copyData.removeAll(myMidData);
                return copyData;
            }
        }).when(dataManagerReader).recoverData(this.myVectorClock);

        List<ClockedData> copyData = new LinkedList<>(myData);
        copyData.removeAll(myMidData);
        List<ClockedData> obtainedData = this.sync.computeFetch(externalClock);
        Assert.assertEquals(copyData, obtainedData);
    }

    @Test
    public void handleHPushTest(){
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);

        int increment = 5;
        List<ClockedData> myData = new LinkedList<>();
        Key lastKey = new StringKey("");

        for (int i = 0; i<increment; i++){
            this.myVectorClock.incrementSelfClock();
            lastKey = new StringKey(Integer.toString(i));
            myData.add(new ClockedData(new VectorClock(this.myVectorClock, 0), lastKey, i));
        }


        ServerHeavyPushMsg heavyPushMsg = new ServerHeavyPushMsg(myData);
        this.sync.handleHeavyPush(heavyPushMsg);

        Mockito.verify(que).addServerData(myData);
    }

    @Test
    public void handleLPushTest(){
        this.sync = new TimeTravel(this.server, this.dataManagerReader, this.connectionManager, this.que);

        int increment = 5;
        List<ClockedData> myData = new LinkedList<>();
        Key lastKey = new StringKey("");

        for (int i = 0; i<increment; i++){
            this.myVectorClock.incrementSelfClock();
            lastKey = new StringKey(Integer.toString(i));
            myData.add(new ClockedData(new VectorClock(this.myVectorClock, 0), lastKey, i));
        }

        VectorClock parallelClock = new VectorClock(this.serverN, 1);
        parallelClock.incrementSelfClock();
        ServerLightPushMsg lightPushMsg1 = new ServerLightPushMsg(this.myVectorClock);
        ServerLightPushMsg lightPushMsg2 = new ServerLightPushMsg(parallelClock);
        Optional<AbstractMsg> abstractMsg1 = this.sync.handleLightPush(lightPushMsg1);
        Optional<AbstractMsg> abstractMsg2 = this.sync.handleLightPush(lightPushMsg2);
        Assert.assertTrue(abstractMsg1.isEmpty());
        Assert.assertTrue(abstractMsg2.isPresent());
    }

    @After
    public void tearDown(){
        sync.stop();
    }
}