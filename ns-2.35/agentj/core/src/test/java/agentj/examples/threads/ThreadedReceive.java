package agentj.examples.threads;

import agentj.AgentJAgent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

public class ThreadedReceive extends AgentJAgent {
    static int count=0;
    int myID;
    InetSocketAddress mcAddr;
    DatagramSocket s;
    int nport = 5555;
    int tick;
    byte[] ibuf = new byte[1500];
    Random rand;
    String msg;
    long jitter;

    public void init() {
        try {
            mcAddr = new InetSocketAddress("-1",nport);
            System.out.println("group="+mcAddr);
            s = new DatagramSocket(nport);
            System.out.println("socket="+s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        rand = new Random(myID * 31415926);
        (new RxTask()).start();
    }

    public ThreadedReceive() {
        ++count;
        myID=count;
    }


    public boolean command(String command, String args[]) {

        System.out.printf("me=%d cmd=%s %s\n",myID,command,args[0]);
        if (command.equals("init")) {
            init();
            return true;
        } else if (command.equals("test")) {
            tick = Integer.parseInt(args[0]);
            jitter = (long) (900 * rand.nextDouble());
            msg = new String("src=" + myID + " jitter=" + jitter + " basetime="+ tick);
            (new TxTask()).start();
            return true;
        }
        return true;
    }

    class TxTask extends Thread {
        public void run() {
            byte [] obuf = new String(msg).getBytes();
            int len = obuf.length;
            try {
                DatagramPacket p = new DatagramPacket(obuf, len, mcAddr );
                Thread.sleep(jitter);
                long mytime=System.currentTimeMillis();
                System.out.printf("me=%d send at %d\n", myID,mytime);
                s.send(p);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
    class RxTask extends Thread {
        public void run() {
            do {
                try {
                    DatagramPacket p = new DatagramPacket(ibuf, ibuf.length);
                    s.receive(p);
                    String msg = new String(p.getData());
                    long mytime=System.currentTimeMillis();
                    System.out.printf("%d recv at %d msg-> %s\n", myID,mytime,msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                };
            } while (true);
        }
    }
}
