package ru.itmo.mit.sd.ptpchat;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.stub.StreamObserver;

public class MessagingServer {
    public class MessagingService extends PeerToPeerMessagingGrpc.PeerToPeerMessagingImplBase {
        @Override
        public void register(Message.PeerDescription request, StreamObserver<Message.PeerDescription> responseObserver) {
            super.register(request, responseObserver);
        }

        @Override
        public void unregister(Message.PeerDescription request, StreamObserver<Message.PeerDescription> responseObserver) {
            super.unregister(request, responseObserver);
        }

        @Override
        public void send(Message.PeerMessage request, StreamObserver<Empty> responseObserver) {
            super.send(request, responseObserver);
        }

        @Override
        public void pollMessageCount(Empty request, StreamObserver<Int32Value> responseObserver) {
            super.pollMessageCount(request, responseObserver);
        }

        @Override
        public void pullMessage(Empty request, StreamObserver<Message.PeerMessage> responseObserver) {
            super.pullMessage(request, responseObserver);
        }
    }
}
