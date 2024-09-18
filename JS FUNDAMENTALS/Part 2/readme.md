## drequired dependencies :
    org.apache.zookeeper

## How to run : 
navigate to the "DistributedSystems" directory. 
Once in the directory run "mvn clean package" to build the project. 
Once the build was successful, navigate to the "target" directory and use the command below in terminal to run the program :
    java -jar service.registry-1.0-SNAPSHOT-jar-with-dependencies.jar 8080 (or whatever port)
This should run the program. 
More instances can be created using the same steps as above to create the worker nodes. 

