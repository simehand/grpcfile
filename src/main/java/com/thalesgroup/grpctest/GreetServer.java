package com.thalesgroup.grpctest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.tp.greeting.GreeterGrpc;
import com.tp.greeting.Greeting.ClientInput;
import com.tp.greeting.Greeting.ServerOutput;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class GreetServer {
 private static final Logger logger = Logger.getLogger(GreetServer.class.getName());
   private Server server;
    
    public static void main(String[] args) throws IOException, InterruptedException { 
        
       final GreetServer greetServer = new GreetServer(); 
       greetServer.start();
       greetServer.server.awaitTermination();
        
    }
    


    private void start() throws IOException {
      int port = 9001;
      //server = ServerBuilder.forPort(port).addService(new GreeterImpl()).build().start();
      server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
        .addService(new GreeterImpl())
        .build()
        .start();
       
      logger.info("GRPC Server started, listening on " + port);
 
      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
            System.err.println("Shutting down gRPC server");
            try {
               server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
               e.printStackTrace(System.err);
            }
         }
      });
   }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
      @Override
      public void greet(ClientInput req, StreamObserver<ServerOutput> responseObserver) {
         logger.info("Got request from client: " + req);
         ServerOutput reply = ServerOutput.newBuilder().setMessage(
            "Server says " + "\"" + req.getGreeting() + " " + req.getName() + "\""
         ).build();
         responseObserver.onNext(reply);
         responseObserver.onCompleted();
      }

    
      @Override
      public StreamObserver<ClientInput> greetstream(StreamObserver<ServerOutput> responseObserver) {

         // TODO Auto-generated method stub
         return new StreamObserver<ClientInput>(){

            int cpt=0;

            @Override
            public void onNext(ClientInput req) {
              String msg= req.getGreeting() + " "+req.getName();
              logger.info("message "+cpt+" "+msg);
              cpt++;
                
            }

            @Override
            public void onError(Throwable t) {
               logger.info("Error while reading book stream: " + t);
                
            }

            @Override
            public void onCompleted() {
           
                // TODO Auto-generated method stub
                ServerOutput reply = ServerOutput.newBuilder().setMessage(
                  "compiled "+ cpt + "messages"
               ).build();
               responseObserver.onNext(reply);
               responseObserver.onCompleted();
               logger.info("computed number of messages "+cpt+" ");
                
            }
         }
      }
   }
}
