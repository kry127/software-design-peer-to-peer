package ru.itmo.mit.sd.ptpchat;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.*;
import ru.itmo.mit.sd.ptpchat.PeerToPeerMessagingGrpc.PeerToPeerMessagingBlockingStub;
import ru.itmo.mit.sd.ptpchat.PeerToPeerMessagingGrpc.PeerToPeerMessagingStub;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample client code that makes gRPC calls to the server.
 */
public class MessagingClient {
    private static final Logger logger = Logger.getLogger(MessagingClient.class.getName());

    private final PeerToPeerMessagingBlockingStub blockingStub;
    private final PeerToPeerMessagingStub asyncStub;

    final private Message.PeerDescription clientDescription;
    private Message.PeerDescription serverDescription;

    /** Construct client for accessing RouteGuide server using the existing channel. */
    public MessagingClient(int port, int ip, String username, Channel channel) {

        clientDescription = Message.PeerDescription.newBuilder()
                .setIp(ip)
                .setPort(port)
                .setName(username)
                .build();
        blockingStub = PeerToPeerMessagingGrpc.newBlockingStub(channel);
        asyncStub = PeerToPeerMessagingGrpc.newStub(channel);
    }

    /**
     * Blocking unary call example.  Calls getFeature and prints the response.
     */
    public void pollMessageCount() {
        info("*** pollMessageCount");

        // build request
        Empty request = Empty.getDefaultInstance();

        Int32Value val;
        try {
            val = blockingStub.pollMessageCount(request);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return;
        }
        info(" server responded available messages: {0}", val.getValue());
    }

    public void register() {
        info("*** register");

        try {
            serverDescription = blockingStub.register(clientDescription);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return;
        }
        info(" Connected to server: {0}", serverDescription);
    }

    public boolean unregister() {
        info("*** register");

        Message.PeerDescription desc;
        try {
            desc = blockingStub.unregister(clientDescription);
            if (desc == serverDescription) {
                serverDescription = null;
                info(" Successfully disconnected from server: {0}", desc);
                return true;
            }
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return false;
        }
        info(" Wrong server: {0}", desc);
        return false;
    }

    public void send(String message) {
        info("*** sending message...");

        Message.PeerMessage msg;
        msg = Message.PeerMessage.newBuilder().setMessage(message).setTimestamp(String.valueOf(new Date())).build();
        try {
            Empty ignore = blockingStub.send(msg);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return;
        }
    }


    public void pullMessage() {
        info("*** receiving message...");

        Empty request = Empty.getDefaultInstance();
        Message.PeerMessage msg;
        try {
            msg = blockingStub.pullMessage(request);
            Program.print_message(serverDescription, msg);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
        }
    }

    /** Issues several different requests and then exits. */
    public static void run(String ip, int port, String username) throws InterruptedException {

        ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
        try {
            MessagingClient client = new MessagingClient(port, Program.ipToInt(ip), username, channel);

            client.pollMessageCount();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private void warning(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
}
