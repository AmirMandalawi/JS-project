package cluster.management;

import org.apache.zookeeper.*;
import java.util.List;

public class ServiceRegistry implements Watcher {
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    public ServiceRegistry(ZookeeperClient zooKeeperClient) {
        this.currentZnode = null;
        this.zooKeeperClient = zooKeeperClient;
        createServiceRegistryPZnode();
    }

    public void registerToCluster(int port) throws KeeperException, InterruptedException {
        // Concat the host name and port number to create the metadata
        // Create an ephemeral sequential znode under /service_registry to be registers
        // to the cluster
        String meta = String.format("%s:%d", "localhost", port);
        String path = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", meta.getBytes());
        this.currentZnode = path.replace(REGISTRY_ZNODE + "/", "");
        System.out.println("Registered to service registry: " + currentZnode);
    }

    public void registerForUpdates() {
        try {
            // Print out all the workers in the cluster
            List<String> workers = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);
            System.out.println("Current cluster workers: " + workers);
        } catch (KeeperException | InterruptedException e) {
        }
    }

    private void createServiceRegistryPZnode() {
        try {
            // Create a persistent znode /service_registry in zookeeper if it doesn't exist
            if (zooKeeperClient.getZookeeper().exists(REGISTRY_ZNODE, false) == null){
                zooKeeperClient.createPersistantNode(REGISTRY_ZNODE, null);
            }
        } catch (KeeperException | InterruptedException e) {
        }
    }

    public void unregisterFromCluster() throws KeeperException, InterruptedException {
        if (currentZnode != null
                && zooKeeperClient.getZookeeper().exists(REGISTRY_ZNODE + "/" + currentZnode, false) != null) {
            // Unregister znode from the cluster
            zooKeeperClient.getZookeeper().delete(REGISTRY_ZNODE + "/" + currentZnode, -1);
            System.out.println("Unregistered from service registry: " + currentZnode);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case NodeChildrenChanged:
                registerForUpdates();
                break;
            default:
                break;
        }
    }
}