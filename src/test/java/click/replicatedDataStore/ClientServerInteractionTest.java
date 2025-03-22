package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.connectionManagers.ClientConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import click.replicatedDataStore.utlis.Key;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClientServerInteractionTest {
    private final String ip = "localhost";
    private final int port = 8080;
    private Logger serverLogger;
    private ClientErrorManager clientLogger;
    @Mock
    private ClientServerPriorityQueue mockedQue;
    @Mock
    private DataManagerReader mockDataRead;

    @Before
    public void setUp(){
        serverLogger = Mockito.mock(Logger.class);
        clientLogger = Mockito.mock(ClientErrorManager.class);
        MockitoAnnotations.openMocks(this);


    }

    @Test
    public void read(){
        // mock loggers
        Mockito.doThrow(new RuntimeException("Server Logger Error triggered")).when(serverLogger).logErr(Mockito.any(), Mockito.any());
        Mockito.doThrow(new RuntimeException("CLint Logger Error triggered")).when(clientLogger).logErr(Mockito.any(), Mockito.any());

        // mock dataReader
        Key key = new StringKey("test");
        int value = 31232354;

        Mockito.doAnswer(new Answer() {
            @Override
            public Serializable answer(InvocationOnMock invocationOnMock) {
                return value;
            }
        }).when(mockDataRead).clientRead(key);

        // declare client and server communication classes
        ClientConnectionManager serverSide = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.serverLogger);
        RequestSender clientSide = new RequestSender(this.ip, this.port, this.clientLogger);
        assertNotNull(serverSide);
        assertNotNull(clientSide);

        // simulate read

        ClientWrite response = clientSide.read(key);

        assert response != null;
        assertEquals((int) response.value(), value);

        serverSide.stop();
        clientSide.disconnect();
    }

    @Test
    public void write(){
        // mock loggers
        Mockito.doThrow(new RuntimeException("Server Logger Error triggered")).when(serverLogger).logErr(Mockito.any(), Mockito.any());
        Mockito.doThrow(new RuntimeException("CLint Logger Error triggered")).when(clientLogger).logErr(Mockito.any(), Mockito.any());

        // mock dataReader
        Key key = new StringKey("test");
        int value = 31232354;
        ClientWrite write = new ClientWrite(key, value);

        Mockito.doAnswer(new Answer() {
            @Override
            public Serializable answer(InvocationOnMock invocationOnMock) {
                return true;
            }
        }).when(mockedQue).addClientData(write);

        // declare client and server communication classes
        ClientConnectionManager serverSide = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.serverLogger);
        RequestSender clientSide = new RequestSender(this.ip, this.port, this.clientLogger);
        assertNotNull(serverSide);
        assertNotNull(clientSide);

        // simulate read

        AnswerState response = clientSide.write(key, value);

        assert response != null;
        assertEquals(AnswerState.OK, response);

        serverSide.stop();
        clientSide.disconnect();
    }
}
