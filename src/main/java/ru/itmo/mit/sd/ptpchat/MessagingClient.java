package ru.itmo.mit.sd.ptpchat;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.*;
import ru.itmo.mit.sd.ptpchat.PeerToPeerMessagingGrpc.PeerToPeerMessagingBlockingStub;
import ru.itmo.mit.sd.ptpchat.PeerToPeerMessagingGrpc.PeerToPeerMessagingStub;
import io.grpc.stub.StreamObserver;

import javax.xml.transform.Result;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
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
    public int pollMessageCount() {
//        info("*** pollMessageCount");

        // build request
        Empty request = Empty.getDefaultInstance();

        Int32Value val;
        try {
            val = blockingStub.pollMessageCount(request);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return 0;
        }
        if (val.getValue() != 0)
            info(" server responded available messages: {0}", val.getValue());
        return val.getValue();
    }

    public boolean register() {
        info("*** register");

        try {
            serverDescription = blockingStub.register(clientDescription);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return false;
        }
        if (serverDescription == null) {
            info(" Cannot connect to server: {0}", serverDescription);
            return false;
        }
        info(" Connected to server: {0}", serverDescription);
        return true;
    }

    public boolean unregister() {
        info("*** unregister");

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


    // --------------------------------------------- thread try ------------------
    private static final Random PRNG = new Random();

    interface TaskR {
        public void run();
    }


    static class TaskReadConsole implements TaskR {
        MessagingClient client = null;
        Scanner sc = null;
        public TaskReadConsole(MessagingClient client, Scanner sc) {
            this.client = client;
            this.sc = sc;
        }
        @Override
        public void run() {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                client.send(line);
            }
        }
    }

    static class TaskReadCounter implements TaskR {
        MessagingClient client = null;
        public TaskReadCounter(MessagingClient client) {
            this.client = client;
        }
        @Override
        public void run() {
            while (true) {
                int new_msg_count = client.pollMessageCount();
                for (int k = 0; k < new_msg_count; k++) {
                    client.pullMessage();
                }
            }
        }
    }

    private static class Result {
        private final int wait;
        public Result(int code) {
            this.wait = code;
        }
    }

    public static Result compute(TaskR obj) throws InterruptedException {
        int wait = PRNG.nextInt(3000);
        obj.run();
        Thread.sleep(wait);
        return new Result(wait);
    }


    /** Issues several different requests and then exits. */
    public static void run(String ip, int port, String username) throws InterruptedException,
            java.util.concurrent.ExecutionException {

        ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();

        Scanner sc = new Scanner(System.in);

        MessagingClient client = null;

        try {
            client = new MessagingClient(port, Program.ipToInt(ip), username, channel);


            TaskReadConsole task1 = new TaskReadConsole(client, sc);
            TaskReadCounter task2 = new TaskReadCounter(client);

            List<Callable<Result>> tasks = new ArrayList<Callable<Result>>();
            tasks.add(new Callable<MessagingClient.Result>() {
                @Override
                public MessagingClient.Result call() throws Exception {
                    return compute(task1);
                }
            });

            tasks.add(new Callable<MessagingClient.Result>() {
                @Override
                public MessagingClient.Result call() throws Exception {
                    return compute(task2);
                }
            });

            ExecutorService exec = Executors.newCachedThreadPool();


            boolean registered = client.register();
            if (!registered) return;

            long start = System.currentTimeMillis();
            List<Future<Result>> results = exec.invokeAll(tasks);
            int sum = 0;
            for (Future<Result> fr : results) {
                sum += fr.get().wait;
                System.out.println(String.format("Task waited %d ms",
                        fr.get().wait));
//            while (true) {
//                int new_msg_count = client.pollMessageCount();
//                if(new_msg_count == 0) {
//                    String line = sc.nextLine();
//                    client.send(line);
//                }
//                client.info("here");
////                int new_msg_count = client.pollMessageCount();
//                for (int k = 0; k < new_msg_count; k++) {
//                    client.pullMessage();
//                }
//            }
            }
        }finally {
            if (client != null) {
                client.unregister();
            }
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
