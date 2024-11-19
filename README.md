Instructions:

1- Build the jar packages by running 
    "mvn clean install" 
    
    Once the maven build is fully complete check the /target directory to check if the client.jar and server.jar have been fully built. 
2- start by running a server with:
    "java -jar target/server.jar" 
 
   The GRPC server should start on port 9001. 

3- Run the client with 
   "java -jar target/client.jar"

   provide the prompted inputs: a server host:port, a  valid file  (see an example in /files directory ) path and confirm whether you want the client  
   to send the file in a single request or in streams of 1kb size of each request message. 

4- You should get the time it took in milliseconds to send the request(s) and received an ack from the server. 
   also the file being sent out should be found in the same place where the server.jar has been run. 
