package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.InjectionUtils;
import click.replicatedDataStore.PortRetryRule;
import click.replicatedDataStore.ServerMock;
import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.connectionThreads.ClientsHandler;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.connectionLayer.messages.ClientReadMsg;
import click.replicatedDataStore.connectionLayer.messages.ClientWriteMsg;
import click.replicatedDataStore.connectionLayer.messages.StateAnswerMsg;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import click.replicatedDataStore.utlis.Key;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.*;


public class ClientConnectionManagerTest {
    private final String ip = "localhost";
    private Logger logger;
    private int port;

    @Mock
    private ClientServerPriorityQueue mockedQue;
    @Mock
    private DataManagerReader mockDataRead;

    @Rule
    public PortRetryRule retryRule = new PortRetryRule(20);

    @Before
    public void setUp(){
        MockitoAnnotations.openMocks(this);
        logger = Mockito.mock(Logger.class);

        port = TestUtils.getPort();
    }

    @Test
    public void successListeningOnPort(){
        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(logger).logErr(Mockito.any(), Mockito.any());

        ClientConnectionManager man = null;

        try {
             man = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.logger);
        }catch (Exception e){
             fail();
        }
        assertNotNull(man);

        man.stop();
    }

    @Test
    public void failListeningOnPort(){
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                System.out.println("failing to open port.\n");
                return null;
            }
        }).when(logger).logErr(Mockito.any(), Mockito.any());

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(logger).logErr(Mockito.any(), Mockito.any());
        });

        ClientConnectionManager man = null;
        BiFunction<ObjectInputStream, ObjectOutputStream, Void> serverFun = (in, out) -> null;
        ServerMock server = new ServerMock(this.port, serverFun);
        server.start();
        t.start();

        try{
            man = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.logger);
            fail();
        }catch (Exception e){
            assertNull(man);
        }

        server.stop();
    }

    @Test
    public void setupConnectionWithClient(){
        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(logger).logErr(Mockito.any(), Mockito.any());

        ClientConnectionManager man = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.logger);
        assertNotNull(man);

        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        Socket socket = null;
        try {
            socket = new Socket(this.ip, this.port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            logger.logErr(this.getClass(),
                    "unable to open socket to " + this.ip + ":" + this.port + "\n" +
                            e.getMessage());
        }
        assertNotNull(socket);
        assertNotNull(in);
        assertNotNull(out);

        int numberElements = ((List<ClientsHandler>) (InjectionUtils.getPrivateField(man, "clientsHandlerList"))).size();
        assertEquals(numberElements, 1);

        man.stop();
    }

    @Test
    public void read(){
        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(logger).logErr(Mockito.any(), Mockito.any());

        ClientConnectionManager man = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.logger);
        assertNotNull(man);

        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        // setup of the input stream clientside
        Socket socket = null;
        try {
            socket = new Socket(this.ip, this.port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
        }

        // simulate read
        Key key = new StringKey("test");
        int value = 31232354;

        Mockito.doAnswer(new Answer() {
            @Override
            public Serializable answer(InvocationOnMock invocationOnMock) {
                return value;
            }
        }).when(mockDataRead).clientRead(key);

        try {
            out.writeObject(new ClientReadMsg(key));
            out.flush();
        }catch (IOException e){
            logger.logErr(this.getClass(),
                    "unable to write the object correctly" + "\n" +
                            e.getMessage());
        }

        ClientWrite response = null;
        try {
            response = ((ClientWriteMsg) in.readObject()).getPayload();
        } catch (IOException ioException){
            logger.logErr(this.getClass(),
                    "unable to read the object correctly" + "\n" + ioException.getMessage());
        } catch (ClassNotFoundException cException) {
            logger.logErr((this.getClass()), "unable to decode correctly the response" + "\n" + cException.getMessage());
        }

        assert response != null;
        assertEquals((int) response.value(), value);

        man.stop();
    }

    @Test
    public void write(){
        Mockito.doThrow(new RuntimeException("Logger Error triggered")).when(logger).logErr(Mockito.any(), Mockito.any());

        ClientConnectionManager man = new ClientConnectionManager(this.port, this.mockedQue, this.mockDataRead, this.logger);
        assertNotNull(man);

        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        // setup of the input stream clientside
        Socket socket = null;
        try {
            socket = new Socket(this.ip, this.port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
        }

        // simulate write
        Key key = new StringKey("test");
        int value = 31232354;
        ClientWrite write = new ClientWrite(key, value);

        Mockito.doAnswer(new Answer() {
            @Override
            public Serializable answer(InvocationOnMock invocationOnMock) {
                return true;
            }
        }).when(mockedQue).addClientData(write);

        try {
            assert out != null;
            out.writeObject(new ClientWriteMsg(write));
            out.flush();
        }catch (IOException e){
            logger.logErr(this.getClass(),
                    "unable to write the object correctly" + "\n" +
                            e.getMessage());
        }

        AnswerState response = null;
        try {
            response = ((StateAnswerMsg) in.readObject()).getPayload();
        } catch (IOException ioException){
            logger.logErr(this.getClass(),
                    "unable to read the object correctly" + "\n" + ioException.getMessage());
        } catch (ClassNotFoundException cException) {
            logger.logErr((this.getClass()), "unable to decode correctly the response" + "\n" + cException.getMessage());
        }

        assert response != null;
        assertEquals(AnswerState.OK, response);

        man.stop();
    }

    @After
    public void tearDown(){

    }
}