# Streamlet


To test the program, we should run the following command in the root of the main folder:
mvn clean package exec:java -Dexec.mainClass="fcul.tdf.Streamlet" -Dexec.args="1 0"
The first argument is related to the node Id, and the second one refers to if a node is trying to reconnect or not.
To test forks, the utils class, has static variables related to the confusion properties, and they should be changed on demand, as requested in the project specification.
Due to my computer performance, for a high number of nodes it becomes hard to have epochs perfectly synchronized.