package section3.p2p;


public class Application {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args){

        try {
            int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
            ZookeeperClient zooKeeperClient = new ZookeeperClient(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT);
            PeerRegistry peerRegistry = new PeerRegistry(zooKeeperClient);
            DHT distributedHashTable = new DHT(zooKeeperClient,peerRegistry,currentServerPort);
            zooKeeperClient.run();
            zooKeeperClient.close();
            System.out.println("Disconnected from Zookeeper, exiting application");
        } catch (Exception ex) {
            System.out.println(ex.toString());

        }
    }
}
