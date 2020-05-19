package ru.itmo.mit.sd.ptpchat;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
    public MessagingServer(int port, int ip, String username) {
        this(port, ip, username, ServerBuilder.forPort(port));
    }

    /**
     * Constructs messaging server instance with specified ServerBuilder
     * @param port attached to server
     * @param serverBuilder user-specified server builder
     */
    public MessagingServer(int port, int ip, String username, ServerBuilder<?> serverBuilder) {
        this.port = port;
        server = serverBuilder.addService(new MessagingService(port, ip, username))
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
        MessagingServer server = new MessagingServer(8980, Program.ipToInt(127, 0, 0, 1), "server_user");
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Inner class 'MessagingService' representing protobuf methods to process incoming messages
     */
    private static class MessagingService extends PeerToPeerMessagingGrpc.PeerToPeerMessagingImplBase {
        private Message.PeerDescription connectedTo;
        final private Message.PeerDescription serverDescription;

        List<Message.PeerMessage> messages;

        MessagingService(int ip, int port,  String name) {
            connectedTo = null;
            serverDescription = Message.PeerDescription.newBuilder()
                    .setIp(ip)
                    .setPort(port)
                    .setName(name)
                    .build();
            messages = new LinkedList<>();
        }

        void pushMessage(Message.PeerMessage msg) {
            messages.add(msg);
        }

        @Override
        public void register(Message.PeerDescription request, StreamObserver<Message.PeerDescription> responseObserver) {
            logger.info("Register called: " + request.getIp());
            if (connectedTo != null) {
                connectedTo = request;
                responseObserver.onNext(serverDescription);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void unregister(Message.PeerDescription request, StreamObserver<Message.PeerDescription> responseObserver) {
            logger.info("Unregister called: " + request.getIp());
            if (connectedTo == null) return;
            if (connectedTo.equals(request)) {
                connectedTo = null;
                responseObserver.onNext(serverDescription);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void send(Message.PeerMessage request, StreamObserver<Empty> responseObserver) {
            logger.info("Send called: " + request.getMessage());
            Program.print_message(connectedTo, request);
            responseObserver.onCompleted(); // empty response
        }

        @Override
        public void pollMessageCount(Empty request, StreamObserver<Int32Value> responseObserver) {
//            super.pollMessageCount(request, responseObserver);
            logger.info("Poll called: ");
            responseObserver.onNext(toInt32Value(messages.size()));
            responseObserver.onCompleted();
        }

        @Override
        public void pullMessage(Empty request, StreamObserver<Message.PeerMessage> responseObserver) {
            super.pullMessage(request, responseObserver);
            logger.info("Pull called: ");
            if (messages.size() == 0) {
                responseObserver.onCompleted(); // no messages yet
                return;
            }
            responseObserver.onNext(messages.get(0));
            messages.remove(0);
            responseObserver.onCompleted(); // no messages yet
        }

        private static Int32Value toInt32Value(int value) {
            return Int32Value.newBuilder().setValue(value).build();
        }
    }
}
