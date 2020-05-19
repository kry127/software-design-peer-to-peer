package ru.itmo.mit.sd.ptpchat;

import org.apache.commons.cli.*;

import java.io.IOException;

public class Program {

    public static void print_message(Message.PeerDescription desc, Message.PeerMessage request) {
        System.out.println(" " + desc.getName() + "[ " + request.getTimestamp() + " ]: " + request.getMessage());
    }

    public static int ipToInt(int a, int b, int c, int d) {
        return a << 24 + b <<  16 + c << 8 + d;
    }

    public static int ipToInt(String ip) {
        if ("localhost".equals(ip)) {
            return ipToInt(127, 0, 0, 1);
        } else {
            // Parse IP parts into an int array
            int[] iparr = new int[4];
            String[] parts = ip.split("\\.");

            for (int i = 0; i < 4; i++) {
                iparr[i] = Integer.parseInt(parts[i]);
            }
            return ipToInt(iparr[0], iparr[1], iparr[2], iparr[3]);
        }
    }

    public static String ipToString(int ip) {
        return (ip >> 24 % 0xFF) + "." + (ip >> 16 % 0xFF) + "." + (ip >> 8 % 0xFF) + "." + (ip % 0xFF);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // create Options object
        Options options = new Options();

        // add t option
        options.addOption("s", "server", false, "Run as a server");
        options.addOption("u", "user", true, "User name");
        options.addOption("a", "address", true, "IP adress of peer");
        options.addOption("p", "port", true, "Port of peer");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            // print help
            return;
        }

        String user = cmd.getOptionValue("u", "anonymous");
        String ip = cmd.getOptionValue("a", "127.0.0.1");
        String port = cmd.getOptionValue("p", "8980");

        int iip = ipToInt(ip);
        int iport = Integer.parseInt(port);

        if (cmd.hasOption("s")) {
            MessagingServer.run(iip, iport, user);
        } else {
            MessagingClient.run(ip, iport, user);
        }
    }
}
