package com.thalesgroup.grpctest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thalesgroup.grpctest.FiletTransferGrpc.FiletTransferImplBase;
import com.thalesgroup.grpctest.Siafiletransfer.FileChunckRequest;
import com.thalesgroup.grpctest.Siafiletransfer.FileChunckResponse;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class FileTransferServer {

    // Logger
    private static final Logger logger = Logger.getLogger(FileTransferClient.class.getName());
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        final FileTransferServer filetTransferServer = new FileTransferServer();
        filetTransferServer.start();
        filetTransferServer.server.awaitTermination();

    }

    private void start() throws IOException {
        int port = 9001;
        // server = ServerBuilder.forPort(port).addService(new
        // GreeterImpl()).build().start();
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .maxInboundMessageSize(Integer.MAX_VALUE)
                .addService(new FileTransferImpl())
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

    static class FileTransferImpl extends FiletTransferImplBase {

        @Override
        public void fullUplaod(FileChunckRequest request, StreamObserver<FileChunckResponse> presponseObserver) {
            long millis = System.currentTimeMillis() % 1000;
               ServerCallStreamObserver<FileChunckResponse> responseObserver =
             (ServerCallStreamObserver<FileChunckResponse>) presponseObserver;
             
            logger.info("Got file rupload equest: " + request.getFilename());
            try {
                responseObserver.setCompression("gzip");
                FileChunckResponse resp = FileChunckResponse.newBuilder().setComplete(true).build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
                logger.info("Completed file reception in " + (millis - (System.currentTimeMillis() % 1000)) + "ms");
                Runnable nonBlockingTask = () -> {
                    try {
                        // Non blocking section
                        FileOutputStream stream = new FileOutputStream(request.getFilename());
                        stream.write(request.getData().toByteArray());
                        stream.close();
                        logger.info("Completed writing file in " +request.getFilename());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                };
                // Submit the task for execution without blocking the main thread
                executor.submit(nonBlockingTask);

            } catch (Throwable e) {
                logger.log(Level.SEVERE, null, e);
            }

        }

        @Override
        public StreamObserver<FileChunckRequest> streamUpload(StreamObserver<FileChunckResponse> pResponseObserver) {

           
            return new StreamObserver<FileChunckRequest>() {
                FileOutputStream stream = null;
                String fileName="";
                long millis = 0;
                ServerCallStreamObserver<FileChunckResponse> responseObserver =
                (ServerCallStreamObserver<FileChunckResponse>) pResponseObserver;

                @Override
                public void onNext(FileChunckRequest chunck) {
                    try {

                        if (chunck.getOffset() == 0) {
                            millis = System.currentTimeMillis() % 1000;
                        }
                        Runnable nonBlockingTask = () -> {
                            try {
                                if (chunck.getOffset() == 0) {
                                    stream = new FileOutputStream(chunck.getFilename());
                                    fileName=chunck.getFilename();
                                }
                                stream.write(chunck.getData().toByteArray());
                            } catch (Exception e) {
                                pResponseObserver.onError(e);
                            }

                        };
                        // Submit the task for execution without blocking the main thread
                        executor.submit(nonBlockingTask);
                        logger.info("Received chunk offset: " + chunck.getOffset());

                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, null, e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.SEVERE, "streaming error ", t);

                }

                @Override
                public void onCompleted() {

                    try {
                        FileChunckResponse resp = FileChunckResponse.newBuilder().setComplete(true).build();
                        
                        responseObserver.setCompression("gzip");
                        responseObserver.onNext(resp);
                        responseObserver.onCompleted();

                        Runnable nonBlockingTask = () -> {
                            try {
                                if (stream != null)
                                    stream.close();
                                
                                logger.info("Completed reception in " + fileName);
                            } catch (Exception e) {
                                responseObserver.onError(e);
                            }

                        };
                        // Submit the task for execution without blocking the main thread
                        executor.submit(nonBlockingTask);
                        logger.info("Completed reception in " + (millis - (System.currentTimeMillis() % 1000)) + "ms");
                    } catch (Throwable e) {
                        responseObserver.onError(e);
                        logger.log(Level.SEVERE, null, e);
                    }

                }

            };
        }

    }

}
