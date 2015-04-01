package pl.edu.agh.dsrg.sr.chat.protocol;

import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ChatProtocolStack extends ProtocolStack {

    public ChatProtocolStack(String address) {

        UDP udp = new UDP();
        if (address != null && !address.equals("")) {
            setUdpAddress(udp, address);
        }
        addProtocol(udp);
        addProtocol(new PING());
        addProtocol(new MERGE2());
        addProtocol(new FD_SOCK());
        addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000));
        addProtocol(new VERIFY_SUSPECT());
        addProtocol(new BARRIER());
        addProtocol(new NAKACK());
        addProtocol(new UNICAST2());
        addProtocol(new STABLE());
        addProtocol(new GMS());
        addProtocol(new UFC());
        addProtocol(new MFC());
        addProtocol(new FRAG2());
        addProtocol(new STATE_TRANSFER());
        addProtocol(new FLUSH());
    }

    private void setUdpAddress(UDP udp, String address) {
        try {
            udp.setValue("mcast_group_addr", InetAddress.getByName(address));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
