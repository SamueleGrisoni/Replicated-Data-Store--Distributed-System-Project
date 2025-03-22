package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.InjectionUtils;
import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.connectionThreads.ClientsHandler;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class ConnectionClientServerInteraction {
    @Mock
    private ClientServerPriorityQueue mockedQue;
    @Mock
    private DataManagerReader mockDataRead;
    @Mock
    private Logger sLogger;
    @Mock
    private ClientErrorManager cLogger;

    private final String ip = "localhost";
    private int port = TestUtils.getPort();

    private RequestSender rq;
    private ClientConnectionManager ccm;

    @Before
    public void setUp(){
        MockitoAnnotations.openMocks(this);
        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(sLogger).logErr(Mockito.any(), Mockito.any());
        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(cLogger).logErr(Mockito.any(), Mockito.any());

        ccm = new ClientConnectionManager(port, mockedQue, mockDataRead, sLogger);
        rq = new RequestSender(ip, port, cLogger);
    }

    @Test
    public void assertSetup(){
        ObjectInputStream cIn = (ObjectInputStream) InjectionUtils.getPrivateField(rq, "in");
        ObjectOutputStream cOut = (ObjectOutputStream) InjectionUtils.getPrivateField(rq, "out");
        List<ClientsHandler> cHandler = (List<ClientsHandler>) InjectionUtils.getPrivateField(ccm, "clientsHandlerList");

        Assert.assertNotNull(cIn);
        Assert.assertNotNull(cOut);
        Assert.assertFalse(cHandler.isEmpty());
    }

    @Test
    public void writeTest(){
        StringKey k = new StringKey("test");
        Integer v = 8;
        rq.write(k, v);

        Mockito.verify(mockedQue).addClientData(new ClientWrite(k, v));
    }

    @Test
    public void readTest(){
        StringKey k = new StringKey("test");
        ClientWrite w = rq.read(k);

        Mockito.verify(mockDataRead).clientRead(k);
        Assert.assertNull(w.value());
    }

    @Test
    public void readAfterWrite(){
        StringKey k = new StringKey("test");
        Integer v = 8;
        rq.write(k, v);

        ClientWrite w = rq.read(k);

        Mockito.verify(mockedQue).addClientData(new ClientWrite(k, v));
        Mockito.verify(mockDataRead).clientRead(k);
        Assert.assertNull(w.value());
    }

    @Test
    public void reconnection() throws Exception{
        // check connection
        ObjectInputStream cIn = (ObjectInputStream) InjectionUtils.getPrivateField(rq, "in");
        ObjectOutputStream cOut = (ObjectOutputStream) InjectionUtils.getPrivateField(rq, "out");
        List<ClientsHandler> cHandler = (List<ClientsHandler>) InjectionUtils.getPrivateField(ccm, "clientsHandlerList");

        Assert.assertNotNull(cIn);
        Assert.assertNotNull(cOut);
        Assert.assertFalse(cHandler.isEmpty());

        rq.disconnect();
        rq = new RequestSender(ip, port, cLogger);

        cIn = (ObjectInputStream) InjectionUtils.getPrivateField(rq, "in");
        cOut = (ObjectOutputStream) InjectionUtils.getPrivateField(rq, "out");
        cHandler = (List<ClientsHandler>) InjectionUtils.getPrivateField(ccm, "clientsHandlerList");

        Thread.sleep(100);
        Assert.assertNotNull(cIn);
        Assert.assertNotNull(cOut);
        Mockito.verifyNoInteractions(sLogger);
        Assert.assertTrue(cHandler.size() == 1);
    }
}
