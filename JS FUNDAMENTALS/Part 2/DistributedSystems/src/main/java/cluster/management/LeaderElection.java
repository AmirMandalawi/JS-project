package cluster.management;

import org.apache.zookeeper.*;

import java.nio.ByteBuffer;

public class LeaderElection implements Watcher {
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private final ZookeeperClient zooKeeperClient;
    private final ServiceRegistry serviceRegistry;
    private final int currentServerPort;
    private String currentZnodeName = null;

    public LeaderElection(ZookeeperClient zooKeeperClient, ServiceRegistry serviceRegistry, int port)
            throws KeeperException, InterruptedException {
        this.zooKeeperClient = zooKeeperClient;
        this.serviceRegistry = serviceRegistry;
        this.currentServerPort = port;
        // Invoke this method to create a persistant znode /leader_election in zookeeper
        // if it doesn't exist
        createElectionRegistryPZnode();
    }

    public void registerCandidacyForLeaderElection() throws KeeperException, InterruptedException {
        // Create a ephermeral sequential node under election znode and assign it to the
        // string currentZnodeName
        byte[] byteArray = ByteBuffer.allocate(4).putInt(currentServerPort).array();
        currentZnodeName = zooKeeperClient.createEphemeralSequentialNode(ELECTION_ZNODE_NAME + "/", byteArray);
        participateInLeaderElection();
    }

    private void participateInLeaderElection() throws KeeperException, InterruptedException {
        // Participate in leader election and determine if the current node is the
        // leader.
        Boolean leader = this.zooKeeperClient.isLeaderNode(ELECTION_ZNODE_NAME, currentZnodeName);
        updateServiceRegistry(leader);
    }

    private void updateServiceRegistry(Boolean isLeader) {
        if (isLeader) {
            onElectedToBeLeader();
        } else {
            onWorker();
        }
    }

    private void createElectionRegistryPZnode() throws KeeperException, InterruptedException {
        // Create a persistant znode /leader_election in zookeeper if it doesn't exist
        if (zooKeeperClient.getZookeeper().exists(ELECTION_ZNODE_NAME, false) == null) {
            zooKeeperClient.createPersistantNode(ELECTION_ZNODE_NAME, null);
        }
    }

    public void onElectedToBeLeader() {
        // Display appropriate message on console - "I am the leader"
        // Invoke necessary methods in ServiceRegistry.java to display the list of
        // worker nodes (ServiceRegistry.registerForUpdates() method)
        // Create the Leader node & register it with /leader_election & /leader_znode
        // ------ FAULT TOLERANCE ------
        // Unregister from the service registry (ServiceRegistry.unregisterFromCluster()
        // method)
        // Watch for any changes(a new worker joining/an existing worker failing) in the
        // cluster through /service_registry znode (ServiceRegistry.registerForUpdates()
        // method)
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.registerForUpdates();
            // Leader watches all the workers
            zooKeeperClient.getZookeeper().getChildren("/service_registry", this);
        } catch (KeeperException | InterruptedException e) {
            System.out.println("Error creating leader znode 1: " + e.getMessage());
        }
        System.out.println("I am the Leader");
    }

    public void onWorker() {
        // Print an appropriate message on console - "I am a worker".
        // Register as a worker in /service_registry znode
        // (ServiceRegistry.registerToCluster(int port) method)
        // ------ FAULT TOLERANCE ------
        // Watch for any changes to the predecessor node, in which case rerun the leader
        // election
        // Register Worker with the Leader_Election
        try {
            String prev = zooKeeperClient.getPredecessorNode(ELECTION_ZNODE_NAME, currentZnodeName);
            zooKeeperClient.getZookeeper().exists(prev, this);
            serviceRegistry.registerToCluster(currentServerPort);
        } catch (KeeperException | InterruptedException e) {
            System.out.println("Error creating worker znode : " + e.getMessage());
        }
        System.out.println("I am a Worker");
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            // if a node fails re-run the participateInLeaderElection()
            case NodeDeleted:
                try {
                    participateInLeaderElection();
                } catch (KeeperException | InterruptedException e) {
                    System.out.println("Error in process method : " + e.getMessage());
                }
                break;
            case NodeChildrenChanged:
                serviceRegistry.registerForUpdates();
                // Reset the watcher
                try {
                    zooKeeperClient.getZookeeper().getChildren("/service_registry", this);
                } catch (KeeperException | InterruptedException e) {
                    System.out.println("Error in process method : " + e.getMessage());
                }
                break;
            default:
                break;
        }
    }
}
