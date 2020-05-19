package ru.itmo.mit.sd.ptpchat;

public class Program {

    public static void print_message(Message.PeerDescription desc, Message.PeerMessage request) {
        System.out.println(" " + desc.getName() + "[ " + request.getTimestamp() + " ]: " + request.getMessage());
    }

    public static int ipToInt(int a, int b, int c, int d) {
        return a << 24 + b <<  16 + c << 8 + d;
    }

    public static String ipToString(int ip) {
        return (ip >> 24 % 0xFF) + "." + (ip >> 16 % 0xFF) + "." + (ip >> 8 % 0xFF) + "." + (ip % 0xFF);
    }

    public static void main(String[] args) {


    }
}
