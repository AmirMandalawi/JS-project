package section3.p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.zookeeper.KeeperException;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DHT {
    private static ZookeeperClient zooKeeperClient;
    private final int port;
    private static HashMap<String, String> map = new HashMap<>();
    private static final String DHT = "/DHT";
    private static String currentZnode = null;
    private static PeerRegistry peerRegistry;
    private String url = "http://localhost:";

    public DHT(ZookeeperClient zooKeeperClient, PeerRegistry peerRegistry, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.peerRegistry = peerRegistry;
        this.port = port;
        this.url += port;
        map = new HashMap<>();
        // Create Permanent DHT node if required & Create Ephemeral Node
        generateDHTZnode();
        // registerDHT(port);
        try {
            launchWeb();
            peerRegistry.registerToCluster(port);
            System.out.println("Launched web server at port " + (port + 10));
        } catch (Exception e) {
            System.out.println("Error launching web server : " + e.toString());
        }
    }

    private void generateDHTZnode() {
        try {
            if (zooKeeperClient.getZookeeper().exists(DHT, false) == null) {
                zooKeeperClient.createPersistantNode(DHT, null);
            }
        } catch (KeeperException | InterruptedException e) {
            System.out.println("Error creating DHT node : " + e.toString());
        }
    }

    private void launchWeb() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port + 10), 0);
        server.createContext("/", exchange -> {
            String response = "";
            if ("GET".equals(exchange.getRequestMethod())) {

                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery();

                if (query != null) {
                    Map<String, String> queryParams = parseQueryParameters(query);
                    String key = queryParams.get("key");
                    String host = peerRegistry.getServer(key);
                    System.out.println(host);
                    if (host.equals(PeerRegistry.sha1(this.url))) {
                        if (map.keySet().contains(PeerRegistry.sha1(key))) {
                            response = map.get(key);
                        } else {
                            response = "Key not found";
                        }
                    } else {
                        String remoteResponse = sendGetRequest(host);
                        if (remoteResponse != null) {
                            // Update the 'response' variable with the response from the remote server
                            response = remoteResponse;
                        } else {
                            // Handle the case where the GET request to the remote server failed
                            response = "Error fetching data from remote server";
                        }
                    }
                } else {
                    response = "GET request does not have query parameters";
                }
            }
            if ("PUT".equals(exchange.getRequestMethod())) {
                InputStream requestBody = exchange.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
                StringBuilder requestBodyStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBodyStr.append(line);
                }
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode jsonNode = objectMapper.readTree(requestBodyStr.toString());
                    String key = jsonNode.get("key").asText();
                    String value = jsonNode.get("value").asText();
                    // Calculate the ID for the received key
                    String keyId = PeerRegistry.sha1(key);
                    // Check if the calculated ID matches the node's own ID
                    if (keyId.equals(PeerRegistry.sha1(this.url))) {
                        // Store the key-value pair in the local hash table (map)
                        map.put(key, value);
                        response = "Received key: " + key + ", value: " + value;
                    } else {
                        // Identify the address where the pair should be stored (as per Zookeeper
                        // information)
                        String targetNodeAddress = peerRegistry.getServer(keyId);

                        if (targetNodeAddress != null) {
                            // Forward the PUT request to the appropriate node
                            String forwardResponse = forwardPutRequest(targetNodeAddress, requestBodyStr.toString());

                            // Process the response from the appropriate node (success/failure)
                            response = processForwardedResponse(forwardResponse);
                        } else {
                            response = "Target node information not found for key: " + key;
                        }
                    }
                    response = "Received key: " + key + ", value: " + value;
                } catch (Exception e) {
                    response = "Error parsing JSON: " + e.getMessage();
                }
            }
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });
        server.start();
    }

    private String processForwardedResponse(String forwardResponse) {
        if (forwardResponse != null) {
            // Check the content of the response to determine success or failure
            if (forwardResponse.contains("success")) {
                return "Successfully stored key-value pair on the remote node.";
            } else if (forwardResponse.contains("failure")) {
                return "Failed to store key-value pair on the remote node.";
            } else {
                return "Received an unknown response from the remote node.";
            }
        } else {
            // Handle the case where the forward request itself failed
            return "Error forwarding the request to the remote node.";
        }
    }

    private String forwardPutRequest(String targetNodeAddress, String string) {
        // Implement the forwardPutRequest method
        try {
            // Create a URL object for the target node's address
            URL url = new URL(targetNodeAddress);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to PUT
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true); // Allow output (i.e., sending data)

            // Write the key-value pair data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = string.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();

            // Check if the request was successful (HTTP status code 200)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the target node
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                // Handle non-200 response codes here
                System.out.println("Forwarded PUT request failed with response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            // Handle exceptions (e.g., network errors) here
            e.printStackTrace();
            return null;
        }
    }

    public String sendGetRequest(String host) {
        try {
            // Create a URL object for the remote server's endpoint
            URL url = new URL(host);
            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Set the request method to GET
            connection.setRequestMethod("GET");
            // Set timeouts (optional)
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(5000); // 5 seconds
            // Get the response code
            int responseCode = connection.getResponseCode();
            // Check if the request was successful (HTTP status code 200)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                // Return the response as a string
                return response.toString();
            } else {
                // Handle non-200 response codes here
                System.out.println("GET request failed with response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            // Handle exceptions (e.g., network errors) here
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> parseQueryParameters(String query) {
        Map<String, String> queryParams = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }

}
