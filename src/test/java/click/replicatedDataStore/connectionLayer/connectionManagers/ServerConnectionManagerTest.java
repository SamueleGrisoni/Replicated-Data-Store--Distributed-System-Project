//todo constructor change fix this tests
//package click.replicatedDataStore.connectionLayer.connectionManagers;
//
//import click.replicatedDataStore.InjectionUtils;
//import click.replicatedDataStore.TestUtils;
//import click.replicatedDataStore.applicationLayer.Server;
//import click.replicatedDataStore.applicationLayer.Logger;
//import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
//import click.replicatedDataStore.connectionLayer.connectionThreads.ServerHandler;
//import click.replicatedDataStore.connectionLayer.messages.*;
//import click.replicatedDataStore.dataStructures.ClockedData;
//import click.replicatedDataStore.dataStructures.Pair;
//import click.replicatedDataStore.dataStructures.VectorClock;
//import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
//import org.junit.*;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.Socket;
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//public class ServerConnectionManagerTest {
//    @Mock
//    private Server server0;
//    @Mock
//    private Server server1;
//    @Mock
//    private Logger logger;
//    @Mock
//    private TimeTravel sync;
//
//    private final String ip = "localhost";
//    private int port0 = TestUtils.getPort();
//    private int port1 = TestUtils.getPort();
//    private int nServer = 2;
//    Map<Integer, Pair<String, Integer>> portsIpIndexes = new HashMap<>();
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(logger).logErr(Mockito.any(), Mockito.any());
//
//        portsIpIndexes.put(0, new Pair<>(ip, port0));
//        portsIpIndexes.put(1, new Pair<>(ip, port1));
//
//        this.initializeServer(0, server0, portsIpIndexes);
//        this.initializeServer(1, server1, portsIpIndexes);
//
//        Mockito.doAnswer(invocationOnMock ->Optional.empty()).when(sync).handleHeavyPush(Mockito.any());
//        Mockito.doAnswer(invocationOnMock ->Optional.empty()).when(sync).handleLightPush(Mockito.any());
//        Mockito.doAnswer(invocationOnMock ->Optional.empty()).when(sync).handleFetch(Mockito.any());
//    }
//
//    private void initializeServer(int index, Server server, Map<Integer, Pair<String, Integer>> serverIpPortMap){
//        Set<Integer> otherIndexes = IntStream.range(0, nServer).boxed().collect(Collectors.toSet());
//        otherIndexes.remove(index);
//        Mockito.doAnswer(invocationOnMock -> index).when(server).getServerIndex();
//        Mockito.doAnswer(invocationOnMock -> nServer).when(server).getNumberOfServers();
//        Mockito.doAnswer(invocationOnMock -> otherIndexes).when(server).getOtherIndexes();
//
//        Mockito.doAnswer(invocationOnMock -> serverIpPortMap.get(index)).when(server).getAddressAndPortsPairOf(index);
//        for(Integer i : otherIndexes)
//            Mockito.doAnswer(invocationOnMock -> serverIpPortMap.get(i)).when(server).getAddressAndPortsPairOf(i);
//    }
//
//    @Test
//    public void startUpConnectionManager() throws IOException, InterruptedException, ClassNotFoundException {
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//
//        Socket socket = new Socket("localhost", port0);
//        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//        out.writeObject(new ServerIndexMsg(1));
//        AnswerState answer = ((StateAnswerMsg) in.readObject()).getPayload();
//        Thread.sleep(100);
//
//        Assert.assertNotNull(answer);
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//    }
//
//    @Test
//    public void connManagerInteraction() throws InterruptedException{
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync, logger, server1);
//        Thread.sleep(100);
//
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn1 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager1, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn1.get(0));
//    }
//
//
//    @Test
//    public void connManagerInteractSendLight() throws InterruptedException{
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//
//        ServerLightPushMsg light = new ServerLightPushMsg(new VectorClock(nServer, 0));
//        connManager0.sendMessage(light, 1);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleLightPush(light);
//    }
//
//    @Test
//    public void connManagerInteractSendHeavy() throws InterruptedException{
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//
//        ClockedData testClockData = new ClockedData(new VectorClock(nServer, 0), new StringKey("test"), 3);
//        List<ClockedData> clockedData = new ArrayList<>();
//        clockedData.add(testClockData);
//        ServerHeavyPushMsg heavy = new ServerHeavyPushMsg(clockedData);
//        connManager0.sendMessage(heavy, 1);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleHeavyPush(heavy);
//    }
//
//    @Test
//    public void connManagerInteractSendFetch() throws InterruptedException{
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//
//        ServerFetchMsg fetch = new ServerFetchMsg(new VectorClock(nServer, 0));
//        connManager0.sendMessage(fetch, 1);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleFetch(fetch);
//    }
//
//    int port2 = TestUtils.getPort();;
//    Server server2 = Mockito.mock(Server.class);
//    TimeTravel sync1 = Mockito.mock(TimeTravel.class);
//    TimeTravel sync2 = Mockito.mock(TimeTravel.class);
//    private void initialize3(){
//        nServer = 3;
//        portsIpIndexes.put(2, new Pair<>(ip, port2));
//        initializeServer(0, server0, portsIpIndexes);
//        initializeServer(1, server1, portsIpIndexes);
//        initializeServer(2, server2, portsIpIndexes);
//
//    }
//
//    @Test
//    public void connManagerInteract3SetUp() throws InterruptedException{
//        int port2 = 8095;
//        Server server2 = Mockito.mock(Server.class);
//        TimeTravel sync1 = Mockito.mock(TimeTravel.class);
//        TimeTravel sync2 = Mockito.mock(TimeTravel.class);
//        nServer = 3;
//        portsIpIndexes.put(2, new Pair<>(ip, port2));
//        initializeServer(0, server0, portsIpIndexes);
//        initializeServer(1, server1, portsIpIndexes);
//        initializeServer(2, server2, portsIpIndexes);
//
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//        ServerConnectionManager connManager2 = new ServerConnectionManager(port2, sync2, logger, server2);
//        Thread.sleep(100);
//
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(2));
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn1 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager1, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn1.get(0));
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn1.get(2));
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn2 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager2, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn2.get(0));
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn2.get(1));
//    }
//
//    @Test
//    public void connManagerInteractBroadcastLight() throws InterruptedException {
//        initialize3();
//
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//        ServerConnectionManager connManager2 = new ServerConnectionManager(port2, sync2, logger, server2);
//        Thread.sleep(100);
//
//        ServerLightPushMsg light = new ServerLightPushMsg(new VectorClock(nServer, 0));
//        connManager0.broadcastMessage(light);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleLightPush(light);
//        Mockito.verify(sync2).handleLightPush(light);
//    }
//
//
//    @Test
//    public void connManagerInteractBroadcastHeavy() throws InterruptedException{
//        initialize3();
//
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//        ServerConnectionManager connManager2 = new ServerConnectionManager(port2, sync2, logger, server2);
//        Thread.sleep(100);
//
//        ClockedData testClockData = new ClockedData(new VectorClock(nServer, 0), new StringKey("test"), 3);
//        List<ClockedData> clockedData = new ArrayList<>();
//        clockedData.add(testClockData);
//        ServerHeavyPushMsg heavy = new ServerHeavyPushMsg(clockedData);
//        connManager0.broadcastMessage(heavy);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleHeavyPush(heavy);
//        Mockito.verify(sync2).handleHeavyPush(heavy);
//    }
//
//    @Test
//    public void connManagerInteractBroadcastFetch() throws InterruptedException{
//        initialize3();
//
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//        ServerConnectionManager connManager2 = new ServerConnectionManager(port2, sync2, logger, server2);
//        Thread.sleep(100);
//
//        ServerFetchMsg fetch = new ServerFetchMsg(new VectorClock(nServer, 0));
//        connManager0.broadcastMessage(fetch);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleFetch(fetch);
//        Mockito.verify(sync2).handleFetch(fetch);
//    }
//
//    @Test
//    public void reConnectionTest() throws InterruptedException {
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync, logger, server1);
//        Thread.sleep(100);
//
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn1 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager1, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn1.get(0));
//
//        ServerLightPushMsg light = new ServerLightPushMsg(new VectorClock(nServer, 0));
//        connManager0.sendMessage(light, 1);
//        Thread.sleep(100);
//        Mockito.verify(sync).handleLightPush(light);
//
//        connManager1.stop();
//        connManager1 = new ServerConnectionManager(port1 + 100, sync1, logger, server1);
//        Thread.sleep(100);
//        serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//        serverHandlerMapOn1 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager1, "serverHandlersMap");
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn1.get(0));
//
//        connManager0.sendMessage(light, 1);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleLightPush(light);
//    }
//
//    @Test
//    public void receiverDownSendMessage() throws InterruptedException {
//        ServerConnectionManager connManager0 = new ServerConnectionManager(port0, sync, logger, server0);
//        Thread.sleep(100);
//
//        Map<Integer, Optional<ServerHandler>> serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//        Assert.assertEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//
//        ServerLightPushMsg light = new ServerLightPushMsg(new VectorClock(nServer, 0));
//        connManager0.sendMessage(light, 1);
//        Thread.sleep(100);
//        Mockito.verifyNoInteractions(sync1);
//
//        ServerConnectionManager connManager1 = new ServerConnectionManager(port1, sync1, logger, server1);
//        Thread.sleep(100);
//
//        serverHandlerMapOn0 = (Map<Integer, Optional<ServerHandler>>) InjectionUtils.getPrivateField(connManager0, "serverHandlersMap");
//
//        Assert.assertNotEquals(Optional.empty(), serverHandlerMapOn0.get(1));
//        connManager0.sendMessage(light, 1);
//        Thread.sleep(100);
//        Mockito.verify(sync1).handleLightPush(light);
//    }
//
//    @After
//    public void tearDown(){
//
//    }
//}