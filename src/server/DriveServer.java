package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


  // Classe principal do servidor.
 // Aguarda conexões de clientes e cria uma thread para cada cliente conectado.

public class DriveServer {

    private static final int PORT = 12345;  // Porta de comunicação do servidor

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor ativo na porta " + PORT);

            // Loop infinito para aceitar conexões
            while (true) {
                Socket clientSocket = serverSocket.accept();  // Espera um cliente se conectar
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                // Cria uma thread para atender o cliente
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
