package org.personnal.client.network;

import java.io.IOException;
import java.net.Socket;

public class ClientConnection implements java.io.Closeable
{
    
    private Socket clientSocket;

    public ClientConnection(String host, int port)throws IOException
    {
        this.clientSocket = new Socket(host, port);
        System.out.println("✅ Connecté au serveur");
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public void close() throws IOException
    {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }
    
}
