package agentj.examples.threads;

/**
 * Created by IntelliJ IDEA.
 * User: scmijt
 * Date: Jul 12, 2006
 * Time: 7:50:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientThread extends java.lang.Thread {

    ThreadedClient threadClient;
    int id;

    public ClientThread(ThreadedClient threadClient, int id) {
        this.threadClient = threadClient;
        this.id = id;
    }

    public void run() {
         System.out.println("Client, Running thread number ---------->  " + id);

         if (threadClient.sockets_enabled){
             threadClient.send("Hello from client on node " + threadClient.nodeid +
                     ", thread id" + id, threadClient.sendToAddress, id);
         } else {
             try {
                 sleep(2000 + (id*2000));    // in milliseconds
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
             System.out.println("Thread returning from node " + threadClient.nodeid + "\n");
         }
     }
}
