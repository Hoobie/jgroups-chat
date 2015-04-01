package pl.edu.agh.dsrg.sr.chat.receiver;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagementMessageReceiver extends ReceiverAdapter {

    private final Map<String, ArrayList<String>> usersConnectedToChannels = new HashMap<>();

    public Map<String, ArrayList<String>> getUsersConnectedToChannels() {
        return usersConnectedToChannels;
    }

    @Override
    public synchronized void receive(Message msg) {
        try {
            ChatAction chatAction = ChatAction.parseFrom(msg.getBuffer());

            ArrayList<String> users = usersConnectedToChannels.get(chatAction.getChannel());
            if (chatAction.getAction() == ActionType.JOIN) {
                addUser(chatAction, users);
            } else {
                removeUser(chatAction, users);
            }

            System.out.println("\t[System] " + chatAction.getNickname() + " " + chatAction.getAction() + " " + chatAction.getChannel() + " channel");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void getState(OutputStream output) throws Exception {
        ChatState.Builder chatStateBuilder = ChatState.newBuilder();
        ChatAction.Builder chatActionBuilder = ChatAction.newBuilder();
        for (Map.Entry<String, ArrayList<String>> entry : usersConnectedToChannels.entrySet()) {
            for (String nick : entry.getValue()) {
                chatStateBuilder.addState(chatActionBuilder.setNickname(nick).setAction(ActionType.JOIN).setChannel(entry.getKey()));
            }
        }
        Util.objectToStream(chatStateBuilder.build(), new DataOutputStream(output));
    }

    @Override
    public synchronized void setState(InputStream input) throws Exception {
        System.out.println("\t[System] Syncing state");
        ChatState inputState = (ChatState) Util.objectFromStream(new DataInputStream(input));
        for (ChatAction chatAction : inputState.getStateList()) {
            ArrayList<String> users = usersConnectedToChannels.get(chatAction.getChannel());
            addUser(chatAction, users);
        }
    }

    @Override
    public synchronized void viewAccepted(View new_view) {
        for (Map.Entry<String, ArrayList<String>> entry : usersConnectedToChannels.entrySet()) {
            List<Address> addresses = new_view.getMembers();
            List<String> members = new ArrayList<>();
            for (Address address : addresses) {
                members.add(address.toString());
            }
            entry.getValue().retainAll(members);
            if (entry.getValue().isEmpty()) {
                usersConnectedToChannels.remove(entry.getKey());
            }
        }
    }

    private void addUser(ChatAction chatAction, ArrayList<String> users) {
        if (users == null) {
            ArrayList<String> list = new ArrayList<>();
            list.add(chatAction.getNickname());
            usersConnectedToChannels.put(chatAction.getChannel(), list);
        } else {
            users.add(chatAction.getNickname());
        }
    }


    private void removeUser(ChatAction chatAction, ArrayList<String> users) {
        users.remove(chatAction.getNickname());
        if (users.isEmpty()) {
            usersConnectedToChannels.remove(chatAction.getChannel());
        }
    }
}
