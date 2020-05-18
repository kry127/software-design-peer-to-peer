package ru.itmo.mit.sd.ptpchat;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class represents server side of the application
 */
public class MessagingServer {

    private static final Logger logger = Logger.getLogger(MessagingServer.class.getName());

    private final int port;
    private final Server server;

    /**
     * Constructs messaging server instance
     * @param port attached to server
     */
    public MessagingServer(int port) {
        this(port, ServerBuilder.forPort(port));
    }

    /**
     * Constructs messaging server instance with specified ServerBuilder
     * @param port attached to server
     * @param serverBuilder user-specified server builder
     */
    public MessagingServer(int port, ServerBuilder<?> serverBuilder) {
        this.port = port;
        server = serverBuilder.addService(new MessagingService())
                .build();
    }

    /**
     * Method starts server
     * @throws IOException
     */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                MessagingServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    /**
     * Method stops server
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Ad-hoc method to easily launch server in IDE. Should not be used in production code
     */
    @Deprecated
    public static void main(String[] args) throws Exception {
        MessagingServer server = new MessagingServer(8980);
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Inner class 'MessagingService' representing protobuf methods to process incoming messages
     */
    private static class MessagingService extends PeerToPeerMessagingGrpc.PeerToPeerMessagingImplBase {
        @Override
        public void register(Message.PeerDescription request, StreamObserver<Message.PeerDescription> responseObserver) {
            super.register(request, responseObserver);
            logger.info("Register called: " + request.getIp());
        }

        @Override
        public void unregister(Message.PeerDescription request, StreamObserver<Message.PeerDescription> responseObserver) {
            super.unregister(request, responseObserver);
            logger.info("Unregister called: " + request.getIp());
        }

        @Override
        public void send(Message.PeerMessage request, StreamObserver<Empty> responseObserver) {
            super.send(request, responseObserver);
            logger.info("Send called: " + request.getMessage());
        }

        @Override
        public void pollMessageCount(Empty request, StreamObserver<Int32Value> responseObserver) {
            super.pollMessageCount(request, responseObserver);
            logger.info("Poll called: ");
        }

        @Override
        public void pullMessage(Empty request, StreamObserver<Message.PeerMessage> responseObserver) {
            super.pullMessage(request, responseObserver);
            logger.info("Pull called: ");
        }
    }
}
