package pl.edu.agh.dsrg.sr.chat.util;

import java.net.InetAddress;

public class ChatUtil {

    private static final String IP_ADDRESS_REGEX = "\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b";

    public static void printUsage() {
        StringBuilder builder = new StringBuilder();
        builder.append("Chat menu: \n")
                .append("\t\\c - create and join a new channel\n")
                .append("\t\\d - disconnect the channel\n")
                .append("\t\\h - help\n")
                .append("\t\\j - join to the channel\n")
                .append("\t\\l - list all channels\n")
                .append("\t\\q - quit");
        System.out.println(builder.toString());
    }

    public static void validateIpAddress(String address) throws Exception {
        if (!address.matches(IP_ADDRESS_REGEX)) {
            throw new IllegalArgumentException("[System] Wrong address format");
        }
        if (!InetAddress.getByName(address).isMulticastAddress()) {
            throw new IllegalArgumentException("[System] Not multicast address");
        }
    }
}
