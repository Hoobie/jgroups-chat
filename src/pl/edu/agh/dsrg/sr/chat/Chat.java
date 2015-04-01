package pl.edu.agh.dsrg.sr.chat;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.stack.ProtocolStack;
import pl.edu.agh.dsrg.sr.chat.protocol.ChatProtocolStack;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatMessage;
import pl.edu.agh.dsrg.sr.chat.receiver.ChatMessageReceiver;
import pl.edu.agh.dsrg.sr.chat.receiver.ManagementMessageReceiver;
import pl.edu.agh.dsrg.sr.chat.util.ChatUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Chat {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String MANAGEMENT_CHANNEL_NAME = "ChatManagement768624";
    private static final int GET_STATE_TIMEOUT = 10000;
    private static final ManagementMessageReceiver managementMessageReceiver = new ManagementMessageReceiver();
    private static String nickname;

    private JChannel managementChannel;
    private Map<String, JChannel> subscribedChannels = new HashMap<>();

    public static void main(String[] args) {
        changeNickname();
        Chat chat = new Chat();
        ChatUtil.printUsage();
        chat.loop();
    }

    public Chat() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        initManagementChannel();
    }

    private void initManagementChannel() {
        managementChannel = new JChannel(false);
        ProtocolStack managementStack = new ChatProtocolStack(null);
        managementChannel.setProtocolStack(managementStack);
        try {
            managementStack.init();
            managementChannel.setName(nickname);
            managementChannel.setReceiver(managementMessageReceiver);
            managementChannel.connect(MANAGEMENT_CHANNEL_NAME);
            managementChannel.getState(null, GET_STATE_TIMEOUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeNickname() {
        nickname = scanForString("\t[System] Enter your nickname: ");
    }

    private void loop() {
        String s = scanForString("");
        switch (s) {
            case "\\c":
                createAndJoinChannel();
                break;
            case "\\d":
                disconnectChannel();
                break;
            case "\\h":
                ChatUtil.printUsage();
                break;
            case "\\j":
                joinChannel();
                break;
            case "\\l":
                listChannels();
                break;
            case "\\q":
                disconnectAll();
                return;
            default:
                writeMessage(s);
        }
        loop();
    }

    private void createAndJoinChannel() {
        JChannel channel = new JChannel(false);
        String nameAndAddress = scanForString("\t[System] Enter channel name (multicast address): ");
        try {
            ChatUtil.validateIpAddress(nameAndAddress);
            ProtocolStack stack = new ChatProtocolStack(nameAndAddress);
            channel.setProtocolStack(stack);
            stack.init();
            channel.setReceiver(new ChatMessageReceiver(nickname, nameAndAddress));
            channel.setName(nickname);
            channel.connect(nameAndAddress);
            subscribedChannels.put(nameAndAddress, channel);
            sendAction(ActionType.JOIN, nameAndAddress);
        } catch (Exception e) {
            channel.close();
            e.printStackTrace();
        }
    }

    private void disconnectChannel() {
        String name = scanForString("\t[System] Enter channel name: ");
        try {
            JChannel channel = subscribedChannels.remove(name);
            sendAction(ActionType.LEAVE, name);
            sleep(1000);
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinChannel() {
        JChannel channel = new JChannel(false);
        String nameAndAddress = scanForString("\t[System] Enter channel name (multicast address): ");
        try {
            ProtocolStack stack = new ChatProtocolStack(nameAndAddress);
            channel.setProtocolStack(stack);
            stack.init();
            channel.setReceiver(new ChatMessageReceiver(nickname, nameAndAddress));
            channel.setName(nickname);
            channel.connect(nameAndAddress);
            sendAction(ActionType.JOIN, nameAndAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        subscribedChannels.put(nameAndAddress, channel);
    }

    private void sendAction(ActionType actionType, String channelName) {
        ChatAction chatAction = ChatAction.newBuilder()
                .setNickname(nickname)
                .setAction(actionType)
                .setChannel(channelName)
                .build();
        byte[] toSend = chatAction.toByteArray();

        try {
            Message msg = new Message(null, null, toSend);
            managementChannel.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listChannels() {
        Map<String, ArrayList<String>> usersConnectedToChannels = managementMessageReceiver.getUsersConnectedToChannels();
        for (Map.Entry<String, ArrayList<String>> entry : usersConnectedToChannels.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (String nick : entry.getValue()) {
                System.out.println("\t- " + nick);
            }
        }
    }

    private void writeMessage(String content) {
        ChatMessage chatMessage = ChatMessage.newBuilder().setMessage(content).build();
        byte[] toSend = chatMessage.toByteArray();

        try {
            for (JChannel channel : subscribedChannels.values()) {
                Message msg = new Message(null, null, toSend);
                channel.send(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disconnectAll() {
        sleep(1000);
        for (JChannel channel : subscribedChannels.values()) {
            sendAction(ActionType.LEAVE, channel.getClusterName());
            channel.close();
        }
        managementChannel.close();
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String scanForString(String message) {
        System.out.print(message);

        if (SCANNER.hasNext()) {
            return SCANNER.nextLine();
        }
        return null;
    }
}
