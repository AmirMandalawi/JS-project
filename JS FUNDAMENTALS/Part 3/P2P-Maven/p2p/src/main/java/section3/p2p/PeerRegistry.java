package section3.p2p;

import org.apache.zookeeper.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class PeerRegistry {
    private static final String REGISTRY_ZNODE = "/peer";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    public PeerRegistry(ZookeeperClient zooKeeperClient) {
        this.zooKeeperClient = zooKeeperClient;
        createPeerRegistryPZnode();
    }

    public void registerToCluster(int port) throws KeeperException, InterruptedException {
        // Concat the host name and port number to create the metadata
        // Create an ephemeral sequential znode under /service_registry to be registers
        // to the cluster
        String meta = String.format("%s:%d", "localhost", port);
        String metaHashed = sha1(meta);
        String path = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", metaHashed.getBytes());
        this.currentZnode = path.replace(REGISTRY_ZNODE + "/", "");
        System.out.println("Registered to peer registry: " + currentZnode);
    }

    private void createPeerRegistryPZnode() {
        try {
            // Create a persistent znode /service_registry in zookeeper if it doesn't exist
            if (zooKeeperClient.getZookeeper().exists(REGISTRY_ZNODE, false) == null) {
                zooKeeperClient.createPersistantNode(REGISTRY_ZNODE, null);
            }
        } catch (KeeperException | InterruptedException e) {
        }
    }

    public String getServer(String key) {
        try {
            List<String> children = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);

            if (children.size() == 0) {
                System.out.println("empty");
                return null;
            }
            String hashedKey = sha1(key);
            int index = Math.abs(hashedKey.hashCode()) % children.size();
            String child = children.get(index);
            byte[] data = zooKeeperClient.getZookeeper().getData(REGISTRY_ZNODE + "/" + child, false, null);

            if (data != null) {
                // Convert the data to a string or any other desired format
                String serverData = new String(data, "UTF-8");
                return serverData;
            }
        } catch (KeeperException | InterruptedException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static String sha1(String input) {
        try {
            // Create a SHA-1 MessageDigest instance
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            // Convert the input string to bytes and update the digest
            byte[] data = input.getBytes();
            sha1Digest.update(data);
            // Calculate the SHA-1 hash
            byte[] hashBytes = sha1Digest.digest();
            // Convert the hash bytes to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xFF & hashByte);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}