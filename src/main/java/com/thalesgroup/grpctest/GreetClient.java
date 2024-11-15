package com.thalesgroup.grpctest;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tp.greeting.GreeterGrpc;
import com.tp.greeting.Greeting.ClientInput;
import com.tp.greeting.Greeting.ServerOutput;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class GreetClient {

   private static final Logger logger = Logger.getLogger(GreetClient.class.getName());
   private final GreeterGrpc.GreeterBlockingStub blockingStub;
   private final GreeterGrpc.GreeterStub stub;

   public GreetClient(Channel channel) {
      blockingStub = GreeterGrpc.newBlockingStub(channel);
      stub=GreeterGrpc.newStub(channel);
   }


   public StreamObserver<ServerOutput> getServerResponseObserver(){

      return new StreamObserver<ServerOutput>(){

         @Override
         public void onNext(ServerOutput value) {
            logger.info("completed : " +value.getMessage());
             // TODO Auto-generated method stub
             
         }

         @Override
         public void onCompleted() {
            logger.info("Notified completion ") ;
             // TODO Auto-generated method stub
             
         }

         @Override
         public void onError(Throwable t) {
            logger.warning(t.getMessage())
             // TODO Auto-generated method stub
             
         }

      
   }
 
   public void makeGreeting(String greeting, String username) {
      logger.info("Sending greeting to server: " + greeting + " for name: " + username);
      ClientInput request = ClientInput.newBuilder().setName(username).setGreeting(greeting).build();
      logger.info("Sending to server: " + request);
      ServerOutput response;
      try {
         response = blockingStub.greet(request);
      } catch (StatusRuntimeException e) {
         logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
         return;
      }
      logger.info("Got following from the server: " + response.getMessage());
   }

   // Normal Unary request/response
   public void sendUnary() throws Exception{

      String greeting = "Ouahib";
      String username = "SIMEHAND";
      String serverAddress = "localhost:9001";
      ManagedChannel channel = ManagedChannelBuilder.forTarget(serverAddress)
            .usePlaintext()
            .build();
      try {
         GreetClient client = new GreetClient(channel);
         client.makeGreeting(greeting, username);
      } finally {
         channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
   }

   //TODO, send streaming
   public void sendStreams(){

   }

   public static void main(String[] args) throws Exception {
    
   }

}