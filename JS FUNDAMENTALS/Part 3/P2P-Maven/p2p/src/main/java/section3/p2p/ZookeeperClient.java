package section3.p2p;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperClient implements Watcher {
    private final ZooKeeper zookeeper;

    public ZookeeperClient(String connectionString, int sessionTimeout) throws IOException {
        this.zookeeper = new ZooKeeper(connectionString, sessionTimeout, this);
    }

    public String createEphemeralSequentialNode(String nodePath, byte[] data)
            throws KeeperException, InterruptedException {
        return this.zookeeper.create(nodePath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    public String createPersistantNode(String nodePath, byte[] data) throws KeeperException, InterruptedException {
        return this.zookeeper.create(nodePath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public List<String> getSortedChildren(String parentPath) throws KeeperException, InterruptedException {
        // Return the list of children of the parent path sorted by its znode index
        // value
        List<String> children = zookeeper.getChildren(parentPath, false);
        Collections.sort(children);
        return children;
    }

    public String getPredecessorNode(String parentZnodeName, String currentZnodeName)
            throws KeeperException, InterruptedException {
        // Return the znode name of the preceeding node
        int index = getSortedChildren(parentZnodeName).indexOf(currentZnodeName);
        return index == 0 ? null : getSortedChildren(parentZnodeName).get(index - 1);
    }

    public boolean isLeaderNode(String parentZnodeName, String currentZnodeName)
            throws KeeperException, InterruptedException {
        // Return True if the current node is the leader node
        return getSortedChildren(parentZnodeName).get(0).equals(currentZnodeName);
    }

    public ZooKeeper getZookeeper() {
        return zookeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zookeeper) {
            zookeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zookeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (getZookeeper()) {
                        System.out.println("Disconnected from Zookeeper event");
                        getZookeeper().notifyAll();
                    }
                }
            default:
                break;
        }
    }
}
