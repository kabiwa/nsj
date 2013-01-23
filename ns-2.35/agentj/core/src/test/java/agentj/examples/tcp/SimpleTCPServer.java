package agentj.examples.tcp;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple TCOP server taken from the knock knock client in the Java tutorial
 *
 * @author Ian Taylor
 */
public class SimpleTCPServer extends AgentJAgent {
    ServerSocket serverSocket = null;

    public SimpleTCPServer() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJDebugLevel.max);                
    }

    public void open() {
        System.out.println("Running Server ...");

         try {

            serverSocket = new ServerSocket(1111);

        } catch (IOException e) {
            System.err.println("Could not listen on port: 0");
            System.exit(1);
        }
    }

    public void acceptConnection() {
        Socket clientSocket = null;
         try {
             clientSocket = serverSocket.accept();

             System.out.println("SimpleTCPServer::acceptConnection - connection accepted !!!!!!!");

             PrintWriter out = new PrintWriter(
                      clientSocket.getOutputStream(), true);
             InputStream in =
                            clientSocket.getInputStream();

             System.out.println("SimpleTCPServer:: Awaiting data !!!!!!!");

             System.out.println("SimpleTCPServer, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );

             int read;
             String message="";

             while ((read=in.read()) != -1)
                message+=(char)read;

             //ANDREW: uncomment this, and comment the while loop above to see the read problem
             // if the byte[] is smaller than the total received data, it works. Otherwise it doesn't
             /*int c;
             byte[] bytes = new byte[256];
             while((c = in.read(bytes, 0, bytes.length)) != -1){
                message += new String(bytes, 0, c);
             }*/
             
             System.out.println("SimpleTCPServer:: received following message from client, sending a Reply !!!!!!!");
             System.out.println("---"+ message +  "---");

             out.println("Ah, Hello client, nice to make your acquaintance...");
             out.flush();
             System.out.println("SimpleTCPServer, Finished");

             out.close();

             clientSocket.close();
             System.out.println("SimpleTCPServer, client socket closed");
             serverSocket.close();
             System.out.println("SimpleTCPServer, serversocket closed");

         } catch (IOException e) {
             System.err.println("Accept failed.");
             System.exit(1);
         }
        System.out.println("SimpleTCPServer::finished ..");

    }

    public boolean command(String command, String args[]) {
        if (command.equals("open")) {
            open();
            return true;
        }
        else if (command.equals("accept")) {
            acceptConnection();
            return true;
        }
        return false;
    }
}
