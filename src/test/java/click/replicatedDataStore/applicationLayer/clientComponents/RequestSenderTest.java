package click.replicatedDataStore.applicationLayer.clientComponents;

import click.replicatedDataStore.InjectionUtils;
import click.replicatedDataStore.PortRetryRule;
import click.replicatedDataStore.ServerMock;
import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.connectionLayer.messages.ClientReadMsg;
import click.replicatedDataStore.connectionLayer.messages.ClientWriteMsg;
import click.replicatedDataStore.connectionLayer.messages.StateAnswerMsg;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import org.junit.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.ErrorManager;

import static org.junit.Assert.*;

public class RequestSenderTest {
    private ClientErrorManager errorManager;
    private int serverPort;
    private String serverIp = "localhost";

    @Rule
    public PortRetryRule retryRule = new PortRetryRule(20);

    @Before
    public void setUp() {
        InjectionUtils.setDebug(true);
        this.errorManager = new ClientErrorManager();

        serverPort = TestUtils.getPort();
    }

    @Test
    public void correctCreateRequestSender() {
        BiFunction<ObjectInputStream, ObjectOutputStream, Void> serverFun = (in, out) -> null;
        ServerMock server = new ServerMock(this.serverPort, serverFun);
        server.start();
        RequestSender requestSender = new RequestSender(this.serverIp, this.serverPort, this.errorManager);

        assertFalse(InjectionUtils.assertPrivateFieldEquals(requestSender, "in", null));
        assertFalse(InjectionUtils.assertPrivateFieldEquals(requestSender, "out", null));

        requestSender.disconnect();
        server.stop();
    }

    @Test
    public void errorCreateRequestSender() {
        RequestSender requestSender = null;
        try {
            requestSender = new RequestSender(this.serverIp, this.serverPort, this.errorManager);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            assertNull(requestSender);
        }

        InjectionUtils.resetDebug();
    }

    @Test
    public void read() {
        StringKey key = new StringKey("key");
        Integer value = 1;

        BiFunction<ObjectInputStream, ObjectOutputStream, Void> serverFun = (in, out) -> {
            try {
                ClientReadMsg input = ((ClientReadMsg) in.readObject());
                assertEquals(key, input.getPayload());
                out.writeObject(new ClientWriteMsg(new ClientWrite(key, value)));
            } catch (Exception e) {
                fail();
            }
            return null;
        };

        ServerMock server = new ServerMock(this.serverPort, serverFun);
        server.start();
        RequestSender requestSender =  new RequestSender(this.serverIp, this.serverPort, this.errorManager);
        ClientWrite result = requestSender.read(key);

        assertEquals(value, result.value());

        requestSender.disconnect();
        server.stop();
    }

    @Test
    public void writeOk() {
        StringKey key = new StringKey("key");
        Integer value = 1;

        BiFunction<ObjectInputStream, ObjectOutputStream, Void> serverFun = (in, out) -> {
            try {
                ClientWriteMsg input = ((ClientWriteMsg) in.readObject());
                assertEquals(key, input.getPayload().key());
                assertEquals(value, input.getPayload().value());
                out.writeObject(new StateAnswerMsg(AnswerState.OK));
            } catch (Exception e) {
                fail();
            }
            return null;
        };

        ServerMock server = new ServerMock(this.serverPort, serverFun);
        server.start();
        RequestSender requestSender = new RequestSender(this.serverIp, this.serverPort, this.errorManager);

        AnswerState result = requestSender.write(key, value);

        assertEquals(AnswerState.OK, result);

        requestSender.disconnect();
        server.stop();
    }

    @Test
    public void writeFail() {
        StringKey key = new StringKey("key");
        Integer value = 1;

        BiFunction<ObjectInputStream, ObjectOutputStream, Void> serverFun = (in, out) -> {
            try {
                ClientWriteMsg input = ((ClientWriteMsg) in.readObject());
                assertEquals(key, input.getPayload().key());
                assertEquals(value, input.getPayload().value());
                out.writeObject(new StateAnswerMsg(AnswerState.FAIL));
            } catch (Exception e) {
                fail();
            }
            return null;
        };

        ServerMock server = new ServerMock(this.serverPort, serverFun);
        server.start();
        RequestSender requestSender = new RequestSender(this.serverIp, this.serverPort, this.errorManager);

        AnswerState result = requestSender.write(key, value);

        assertEquals(AnswerState.FAIL, result);

        requestSender.disconnect();
        server.stop();
    }

    @After
    public void tearDown() {
        InjectionUtils.resetDebugAll();
        this.errorManager = null;
    }
}