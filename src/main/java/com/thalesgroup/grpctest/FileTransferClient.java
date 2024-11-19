package com.thalesgroup.grpctest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import com.thalesgroup.grpctest.Siafiletransfer.FileChunckRequest;
import com.thalesgroup.grpctest.Siafiletransfer.FileChunckResponse;

import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.stub.StreamObserver;

public class FileTransferClient {

    private static final Logger logger = Logger.getLogger(FileTransferClient.class.getName());

    private final FiletTransferGrpc.FiletTransferBlockingStub blockingStub;
    private final FiletTransferGrpc.FiletTransferStub stub;
    private StreamObserver<FileChunckRequest> streamClientSender;
    private boolean serverResponseCompleted = false;
    private long millis = 0;

    public FileTransferClient(Channel channel) {
        blockingStub = FiletTransferGrpc.newBlockingStub(channel);
        stub = FiletTransferGrpc.newStub(channel);

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            /// Enter data using BufferReader

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));
            System.out.println("\n Enter remote server, format: <host:port> : ");
            // Reading server
            String server = reader.readLine();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(server)
                    .usePlaintext()
                    .maxInboundMessageSize(Integer.MAX_VALUE)
                    .build();

            FileTransferClient client = new FileTransferClient(channel);

            System.out.println("\n Enter  file path :");
            // Reading file path
            String filePath = reader.readLine();

            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("\n  File path : " + filePath + "  doesn't exist, enter a valid path :");
                return;
            }

            System.out.println("\n Do you want to send streams (Y or N) :");
            // Reading file path
            String sendStream = reader.readLine();

            // Printing the read line
            System.out.println("Server address: " + server + ", file path" + filePath);

            // String filePath = "/Users/asimehand/Documents/grpcfile/files/docs.pdf";
            // String server = "127.0.0.1:9001";
            // String sendStream = "Y";

            if (sendStream.equals("Y"))
                client.sendStream(file.getAbsolutePath(), file.getName());
            else
                client.sendUnary(file.getAbsolutePath(), file.getName());

        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    // Normal Unary request/response
    public void sendUnary(String filePath, String fileName) throws IOException, InterruptedException {
        try {

            LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
            // read the file and convert to a byte array
            InputStream inputStream = new FileInputStream(filePath);

            byte[] bytes = inputStream.readAllBytes();
            int length = bytes.length;

            FileChunckRequest req = FileChunckRequest.newBuilder()
                    .setData(ByteString.copyFrom(bytes, 0, length))
                    .setOffset(0)
                    .setFilename(fileName)
                    .setSize(length)
                    .setOffset(0)
                    .build();
            millis = System.currentTimeMillis() % 1000;
            logger.info("\n Start uploading .......");
            FileChunckResponse resp = blockingStub.withCompression("gzip").fullUplaod(req);
            logger.log(Level.INFO, "Ack received: {0}", resp.getComplete());
            if (resp.getComplete()) {
                logger.info("Completed upload in " + (millis - (System.currentTimeMillis() % 1000)) + "ms");
            }

            inputStream.close();
            millis = 0;
        } finally {
            ((ManagedChannel) stub.getChannel()).shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }

    }

    // Send streams
    public void sendStream(String filePath, String fileName) throws InterruptedException {

        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());

        if (streamClientSender == null) {
            streamClientSender = stub.withCompression("gzip").streamUpload(getStreamObserver());
        }

        FileChunckRequest req = null;

        try {

            // read the file and convert to a byte array
            InputStream inputStream = new FileInputStream(filePath);

            byte[] bytes = inputStream.readAllBytes();
            BufferedInputStream imageStream = new BufferedInputStream(new ByteArrayInputStream(bytes));

            int bufferSize = 1 * 1024;// 1K
            byte[] buffer = new byte[bufferSize];
            int length;
            logger.info("Send file in stream of bytes " + fileName + " ....... ");
            int offset = 0;
            millis = System.currentTimeMillis() % 1000;
            while ((length = imageStream.read(buffer, 0, bufferSize)) != -1) {
                req = FileChunckRequest.newBuilder()
                        .setData(ByteString.copyFrom(buffer, 0, length))
                        .setOffset(0)
                        .setFilename((offset == 0) ? fileName : "")
                        .setSize(length)
                        .setOffset(offset)
                        .build();

                streamClientSender.onNext(req);
                System.out.println("Offset  " + offset + " sent");
                offset++;

            }
            // Mark the end of requests
            streamClientSender.onCompleted();

            while (this.serverResponseCompleted == false) {
                Thread.sleep(2000);
            }

            inputStream.close();
            serverResponseCompleted = false;

        } catch (Throwable t) {
            logger.log(Level.SEVERE, filePath, t);
        } finally {
            ((ManagedChannel) stub.getChannel()).shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }

    }

    public StreamObserver<FileChunckResponse> getStreamObserver() {

        return new StreamObserver<Siafiletransfer.FileChunckResponse>() {

            @Override
            public void onNext(FileChunckResponse resp) {
                logger.info("Ack from server " + resp.getComplete() + " , Completed upload in "
                        + (millis - (System.currentTimeMillis() % 1000)) + "ms");

            }

            @Override
            public void onCompleted() {
                serverResponseCompleted = true;
                millis = 0;
                System.out.println("COMPLETE!");
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.SEVERE, "GRPC Error ", t);

            }

        };
    }

    public void transferFileStream(String filePath) {
        try {

            // read the file and convert to a byte array
            InputStream inputStream = new FileInputStream(filePath);

            byte[] bytes = inputStream.readAllBytes();
            BufferedInputStream imageStream = new BufferedInputStream(new ByteArrayInputStream(bytes));

            int bufferSize = 1 * 1024;// 1K
            byte[] buffer = new byte[bufferSize];
            int length;
            FileOutputStream stream = new FileOutputStream("output.pdf");
            while ((length = imageStream.read(buffer, 0, bufferSize)) != -1) {

                System.out.println(buffer.toString() + "   " + length);
                stream.write(buffer);

            }

            inputStream.close();
            stream.close();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, filePath, t);
        }

    }
}
