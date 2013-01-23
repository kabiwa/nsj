package agentj.examples.udp;

import agentj.AgentJAgent;
import agentj.NAMCommands;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * @author Ian Taylor
 *         <p/>
 *         Simple multicast class that works in AgentJ and NS2 - see the
 *         SimpleMulticast.tcl script in the examples directory
 */

public class ThreadedMulticast extends AgentJAgent {
    public InetSocketAddress group;
    private MulticastSocket s;
    int port = 427;
    private Thread server;
    private boolean sa = false;
    private ThreadPool exec;
    private ScheduledThread timer;

    public static final String PREFIX_DA = "service:directory-agent:";
    public static final String PREFIX_DA_URL = PREFIX_DA + "//";
    public static final String PREFIX_SA = "service:service-agent:";
    public static final String PREFIX_SA_URL = PREFIX_SA + "//";


    private boolean running = false;

    public ThreadedMulticast() {
        this.setJavaDebugLevel(Logger.LogLevel.ALL);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
    }

    public void init() {
        group = new InetSocketAddress("228.5.6.7", port);
        try {
            System.out.println("SimpleMulticast, running on node " + InetAddress.getLocalHost().getHostName());
            System.out.println("SimpleMulticast, Group Address Allocated = " + group.getHostName());
            s = new MulticastSocket(port);
            s.joinGroup(group, null);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        exec = new ThreadPool(5);


    }

    public List<String> discoverSAs() {
        ThreadedMulticast.this.getNamCommands().setNodeLabel("Looking for SAs", NAMCommands.LabelPosition.downright, NAMCommands.NamColor.chocolate);
        return multicastSend(PREFIX_SA);
    }

    public List<String> discoverDAs() {
        ThreadedMulticast.this.getNamCommands().setNodeLabel("Looking for DAs", NAMCommands.LabelPosition.downright, NAMCommands.NamColor.blue);
        return multicastSend(PREFIX_DA);
    }

    public void discoverService() {
        List<String> sas = discoverDAs();
        if(sas.size() > 0) {
            System.out.println("ThreadedMulticast.discoverService found some DAs on the network!! hmm.");
        } else {
            System.out.println("$$$$$$$ThreadedMulticast.discoverService NO DAs Found in network...");
        }
        sas = discoverSAs();
        for (int i = 0; i < sas.size(); i++) {
            String s1 = sas.get(i);
            if (s1.startsWith(PREFIX_SA_URL)) {
                String host = s1.substring(PREFIX_SA_URL.length(), s1.length());
                System.out.println("ThreadedMulticast.discoverService sending unicast to host " + host);
                List<String> services = unicastSend("service:tcp", host);
                for (int j = 0; j < services.size(); j++) {
                    String s2 = services.get(j);
                    ThreadedMulticast.this.getNamCommands().setNodeLabel("Discovered Service!!!!!", NAMCommands.LabelPosition.downright, NAMCommands.NamColor.chocolate);

                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!ThreadedMulticast.discoverService discovered!!!!!!!!!!!!!!!!!!!!!!!!!" + s2);
                }
            }
        }

    }

    public void setSA() {
        this.sa = true;

    }

    public List<String> unicastSend(String msg, String host) {
        try {
            return send(msg, new DatagramPacket(msg.getBytes(), msg.length(), new InetSocketAddress(host, port)));
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("ThreadedMulticast.mulitcastSend returning null");
        return null;
    }

    public List<String> multicastSend(String msg) {
        try {
            return send(msg, new DatagramPacket(msg.getBytes(), msg.length(), group));
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("ThreadedMulticast.mulitcastSend returning null");
        return null;
    }


    public List<String> send(String msg, DatagramPacket packet) {

        try {
            System.out.println("SimpleMulticast sending = " + msg);
            UDPReceiver r = new UDPReceiver();
            exec.execute(r);
            r.getSocket().send(packet);
            int count = 0;
            int waitTime = 250;
            int totalWait = 0;
            while (r.getResults().size() == 0 && count < 4) {
                System.out.println("ThreadedMulticast.send count=" + count);
                try {
                    Thread.sleep(waitTime);
                    count++;
                    totalWait += waitTime;
                    waitTime *= 2;
                    if(r.getResults().size() == 0 && count == 2) {
                        break;
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            System.out.println("ThreadedMulticast.send waited in total for " + totalWait);
            r.close();
            return r.getResults();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>();
    }


    public void stop() {
        running = false;
        server.interrupt();
        exec.shutdown();
        //NOTE: if not doing timer - comment out
        timer.shutdown();

    }

    public void receive() {
        //NOTE: if not doing timer - comment out
        timer = new ScheduledThread(this, 1000, 1000);
        timer.execute();
        running = true;
        server = new Thread() {
            public void run() {
                byte[] buf = new byte[1000];
                while (running && !interrupted()) {
                    final DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    try {
                        System.out.println("SimpleMulticast blocking on receive");
                        s.receive(recv);
                        String req = new String(recv.getData());
                        System.out.println("SimpleMulticast received -------------------------------------- " + req +
                                " at " + System.currentTimeMillis());
                        System.out.println("ThreadedMulticast.run packet received from " + ((InetSocketAddress) recv.getSocketAddress()).getHostName() +
                                " on " + InetAddress.getLocalHost());
                        if (sa) {
                            if (req.startsWith(PREFIX_SA)) {
                                System.out.println("ThreadedMulticast.run returning sa information");
                                exec.execute(new SaInfo(recv));
                            } else if(req.startsWith("service:tcp")){
                                exec.execute(new SrvInfo(recv));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    s.leaveGroup(group, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("SimpleMulticast.run exiting server thread.");
            }
        };
        server.start();

    }

    public static class UDPReceiver implements Runnable {

        private List<String> results = new Vector<String>();
        private DatagramSocket socket;
        private boolean stopped = false;

        public UDPReceiver() {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            byte[] buf = new byte[1000];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            try {
                while(!stopped) {
                    socket.receive(dp);
                    //ThreadedMulticast.this.getNamCommands().setNodeLabel("received UDP", NAMCommands.LabelPosition.downright, NAMCommands.NamColor.green);
                    results.add(new String(dp.getData()));
                    System.out.println("received response ------------------------------------------" + new String(dp.getData()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            Thread.currentThread().interrupt();
            stopped = true;
            socket.close();

        }

        public List<String> getResults() {
            return Collections.unmodifiableList(results);
        }

        public DatagramSocket getSocket() {
            return socket;
        }
    }

    public class ScheduledDADiscovery extends TimerTask {

        public void run() {
            System.out.println("---------ThreadedMulticast$ScheduledDADiscovery.run executed");
            List<String> das = discoverDAs();
            for (int i = 0; i < das.size(); i++) {
                String s1 = das.get(i);
                System.out.println("ThreadedMulticast$ScheduledDADiscovery.run received DA srting " + s1);
            }
        }
    }


    public class SaInfo implements Runnable {
        private DatagramPacket packet;

        SaInfo(DatagramPacket packet) {
            this.packet = packet;
        }

        public void run() {
            try {
                String msg = PREFIX_SA_URL + InetAddress.getLocalHost();
                byte[] bytes = msg.getBytes();
                ThreadedMulticast.this.getNamCommands().setNodeLabel("Returning SA info", NAMCommands.LabelPosition.downright, NAMCommands.NamColor.purple);

                DatagramPacket send = new DatagramPacket(bytes, bytes.length, packet.getSocketAddress());
                System.out.println("ThreadedMulticast.saInfo sending unicast info to " + ((InetSocketAddress) packet.getSocketAddress()).getHostName() +
                        " on groupPort1 " + ((InetSocketAddress) packet.getSocketAddress()).getPort());
                new DatagramSocket().send(send);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class SrvInfo implements Runnable {
        private DatagramPacket packet;

        public SrvInfo(DatagramPacket packet) {
            this.packet = packet;
        }

        public void run() {
            try {
                String msg = "service:tcp://" + InetAddress.getLocalHost() + "/bla";
                byte[] bytes = msg.getBytes();
                ThreadedMulticast.this.getNamCommands().setNodeLabel("Returning Service info", NAMCommands.LabelPosition.downright, NAMCommands.NamColor.purple);

                DatagramPacket send = new DatagramPacket(bytes, bytes.length, packet.getSocketAddress());
                System.out.println("ThreadedMulticast.saInfo sending unicast info to " + ((InetSocketAddress) packet.getSocketAddress()).getHostName() +
                        " on groupPort1 " + ((InetSocketAddress) packet.getSocketAddress()).getPort());
                new DatagramSocket().send(send);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            init();
            return true;
        } else if (command.equals("discover")) {
            discoverService();
            return true;
        } else if (command.equals("receive")) {
            receive();
            return true;
        } else if (command.equals("stop")) {
            stop();
            return true;
        } else if (command.equals("sa")) {
            setSA();
            return true;
        }
        return false;
    }

}