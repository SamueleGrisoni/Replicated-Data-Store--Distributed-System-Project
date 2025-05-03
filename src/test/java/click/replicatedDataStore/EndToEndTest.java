package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EndToEndTest {
    private final String ip = "localhost";
    private final ScheduledExecutorService clients = Executors.newScheduledThreadPool(3);

    private Map<Integer, Pair<String, ServerPorts>> initializeNAddressMap(int serverNum){
        Map<Integer, Pair<String, ServerPorts>> address = new HashMap<>();

        for(int i = 0; i < serverNum; i++){
            address.put(i, new Pair<>(this.ip, new ServerPorts(TestUtils.getPort(), TestUtils.getPort())));
        }

        return address;
    }

    private Map<Integer, Server> initializeNServers(Map<Integer, Pair<String, ServerPorts>> serverAddresses){
        Map<Integer, Server> serverMap = new HashMap<>();

        for(int i = 0; i < serverAddresses.size(); i++){
            Server server = new Server("serverTest_" + i, i, serverAddresses);
            serverMap.put(i, server);
        }

        return serverMap;
    }

    private Map<Integer, RequestSender> connectOneClientPerServer(Map<Integer, Server> serverMap,
                                                   Map<Integer, Pair<String, ServerPorts>> addresses){
        Map<Integer, RequestSender> clientMap = new HashMap<>();
        ClientErrorManager clientErrorLogger = new ClientErrorManager();

        for(int i = 0; i < serverMap.size(); i++){
            RequestSender sender = new RequestSender(this.ip, addresses.get(i).second().clientPort(), clientErrorLogger);
            clientMap.put(i, sender);
        }

        return clientMap;
    }

    StringKey getKeyFromIndex(int i){
        return new StringKey("key" + i);
    }

    Integer getValueFromIndex(int i){
        return i;
    }

    private void sendNWrites(RequestSender client, int startingValue, int N){
        for(int i = startingValue; i < N; i++){
            client.write(getKeyFromIndex(i), getValueFromIndex(i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendWrites(Map<Integer, RequestSender> clientMap, int writesPerClient){
        clientMap.forEach(((integer, requestSender) -> {
            Thread t = new Thread(() -> {
                int N = writesPerClient;
                this.sendNWrites(requestSender, integer * N, N);
            });
            t.start();
        }));
    }

    private void readAndAssertReads(RequestSender client, int start, int end){
        for(int i = start; i < end; i++){
            ClientWrite read = client.read(getKeyFromIndex(i));
            Assert.assertEquals(read.value(), getValueFromIndex(i));
        }
    }

    private void assertAllClientsReads(Map<Integer, RequestSender> clientMap, int start, int end){
        clientMap.forEach(((integer, requestSender) -> {
            readAndAssertReads(requestSender, start, end);
        }));
    }


    @Test
    public void readWriteStressTest() throws InterruptedException {
        Map<Integer, Pair<String, ServerPorts>> addresses = initializeNAddressMap(3);
        Map<Integer, Server> serverMap = initializeNServers(addresses);
        serverMap.forEach(((integer, server) -> server.start()));
        Thread.sleep(100);
        Map<Integer, RequestSender> clientMap = connectOneClientPerServer(serverMap, addresses);
        int NUM_OF_WRITES = 100;
        sendWrites(clientMap, NUM_OF_WRITES);

        Thread.sleep(2000);
        assertAllClientsReads(clientMap, 0, clientMap.size() * NUM_OF_WRITES);
    }
}
