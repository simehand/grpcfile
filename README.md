Instructions:

1- Build the jar file by running 
    mvn clean install 

2- start by running a server with:
    java -jar server.jar 
 
   The GRPC server should start on port 9001. 

3- Run the client with 
   java -jar client 

   provide the prompted inputs: a server host:port, a  valid file path and confirm whether you want the client  
   to send the file in a single request or in streams of 1kb size of each request message. 

4- You should get the time it took in milliseconds to send the request(s) and received an ack from the server. 
   also the file being sent out should be found in the same place where the server.jar has been run. 