package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;
import click.replicatedDataStore.utils.configs.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EndToEndTest {
    private final String ip = "localhost";
    Map<Integer, Pair<String, ServerPorts>> addresses;
    Map<Integer, Server> serverMap;
    Map<Integer, RequestSender> clientMap;
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
            Server server = new Server("serverTest_" + i, i, serverAddresses, true);
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
        for(int i = startingValue; i < startingValue + N; i++){
            client.write(getKeyFromIndex(i), getValueFromIndex(i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendWrites(Map<Integer, RequestSender> clientMap, int writesPerClient){
        clientMap.forEach(((clientID, requestSender) -> {
            Thread t = new Thread(() -> {
                int N = writesPerClient;
                this.sendNWrites(requestSender, clientID * N, N);
            });
            t.start();
        }));
    }

    private void readAndAssertReads(int clientID, RequestSender client, int start, int end){
        for(int i = start; i < end; i++){
            System.out.println("Client: " + clientID + " reading key: " + getKeyFromIndex(i));
            ClientWrite read = client.read(getKeyFromIndex(i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Assert.assertEquals(read.value(), getValueFromIndex(i));
        }
    }

    private void assertAllClientsReads(Map<Integer, RequestSender> clientMap, int start, int end){
        clientMap.forEach(((clientID, requestSender) -> {
            readAndAssertReads(clientID, requestSender, start, end);
        }));
    }

    private void startServer(){
        serverMap.forEach(((integer, server) -> server.start()));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDataFolder(File dataFolder){
        System.out.println("Deleting data folder: " + dataFolder.getAbsolutePath());
        if (dataFolder.exists()) {
            for (File file : dataFolder.listFiles()) {
                if (!file.isDirectory()) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete file: " + file.getAbsolutePath());
                    } else {
                        System.out.println("Deleted file: " + file.getAbsolutePath());
                    }
                }else{
                    deleteDataFolder(file);
                }
            }
        } else {
            System.out.println("Data folder does not exist: " + dataFolder.getAbsolutePath());
        }
    }

    private void turnOffServer(int serverID) {
        serverMap.get(serverID).stopServer();
        serverMap.remove(serverID);
        addresses.remove(serverID);

        clientMap.get(serverID).disconnect();
        clientMap.remove(serverID);
        System.out.println("Server " + serverID + " turned off");
    }

    private void turnOnServer(int serverID) {
        addresses.put(serverID, new Pair<>(this.ip, new ServerPorts(TestUtils.getPort(), TestUtils.getPort())));
        Server restartedServer = new Server("serverTest_" + serverID, serverID, addresses, true);
        restartedServer.start();
        serverMap.put(serverID, restartedServer);

        ClientErrorManager clientErrorLogger = new ClientErrorManager();
        RequestSender sender = new RequestSender(this.ip, addresses.get(serverID).second().clientPort(), clientErrorLogger);
        clientMap.put(0, sender);
        System.out.println("Server " + serverID + " turned on");
    }

    @Test
    public void sendWritesAndReadsTest() throws InterruptedException {
        deleteDataFolder(new File(ServerConfig.getGlobalFolderPath()));
        addresses = initializeNAddressMap(3);
        serverMap = initializeNServers(addresses);
        startServer();
        clientMap = connectOneClientPerServer(serverMap, addresses);

        //each client writes 1 key-value pair
        int NUM_OF_WRITES = 1;
        sendWrites(clientMap, NUM_OF_WRITES);

        Thread.sleep(2000);
        assertAllClientsReads(clientMap, 0, clientMap.size() * NUM_OF_WRITES);

        int serverShutDownIndex = 0;
        turnOffServer(serverShutDownIndex);
        Thread.sleep(1000);

        //send writes between the remaining servers
        sendWrites(clientMap, NUM_OF_WRITES);
        Thread.sleep(1000);
        assertAllClientsReads(clientMap, 0, (clientMap.size()+1) * NUM_OF_WRITES);

        //turn on the server that was turned off
        turnOnServer(serverShutDownIndex);
        Thread.sleep(2000);

        assertAllClientsReads(clientMap, 0, (clientMap.size()) * NUM_OF_WRITES);
    }
}
