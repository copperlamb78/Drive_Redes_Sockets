package server;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * Classe responsável por gerenciar cada cliente que se conecta ao servidor.
 * Cada cliente terá uma instância dessa classe rodando em uma Thread separada.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;

    // Lista de usuários e senhas válidos para autenticação.
    private static final HashMap<String, String> users = new HashMap<String, String>() {{
        put("usuario1", "senha1");
        put("usuario2", "senha2");
    }};

    // Construtor da classe que recebe o socket do cliente conectado.
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
        ) {
            // Inicia o processo de login quando o cliente se conecta.
            realizarLogin(dis, dos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Realiza o processo de login solicitando nome de usuário e senha.
     * Se for autenticado, permite que o cliente acesse as funcionalidades.
     */
    private void realizarLogin(DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeUTF("Usuario:");
        String username = dis.readUTF();
        dos.writeUTF("Senha:");
        String password = dis.readUTF();

        if (autenticarUsuario(username, password)) {
            dos.writeUTF("LOGIN_SUCESSO");
            criarPastasUsuario(username);
            interagirComCliente(dis, dos, username);
        } else {
            dos.writeUTF("LOGIN_FALHOU");
        }
    }

    /**
     * Valida se o nome de usuário e senha recebidos são válidos.
     */
    private boolean autenticarUsuario(String user, String pass) {
        return users.containsKey(user) && users.get(user).equals(pass);
    }

    /**
     * Cria as pastas para o usuário caso seja o primeiro acesso.
     * Cada usuário possui subpastas para PDF, JPG e TXT.
     */
    private void criarPastasUsuario(String user) {
        String[] tipos = {"pdf", "jpg", "txt"};
        for (String tipo : tipos) {
            File pasta = new File("armazenamento/" + user + "/" + tipo);
            if (!pasta.exists()) pasta.mkdirs();
        }
    }

    /**
     * Recebe comandos do cliente e executa as ações de listar, enviar, baixar ou sair.
     */
    private void interagirComCliente(DataInputStream dis, DataOutputStream dos, String user) throws IOException {
        while (true) {
            String comando = dis.readUTF();

            switch (comando) {
                case "LIST":
                    listarArquivosUsuario(dos, user);
                    break;
                case "UPLOAD":
                    receberArquivo(dis, user);
                    break;
                case "DOWNLOAD":
                    enviarArquivo(dis, dos, user);
                    break;
                case "EXIT":
                    socket.close();
                    return;
                default:
                    dos.writeUTF("Comando inválido");
            }
        }
    }

    /**
     * Lista todos os arquivos do usuário, separados por tipo.
     * O resultado é enviado para o cliente como texto.
     */
    private void listarArquivosUsuario(DataOutputStream dos, String user) throws IOException {
        File pastaUsuario = new File("armazenamento/" + user);
        StringBuilder lista = new StringBuilder();

        for (File tipoPasta : pastaUsuario.listFiles()) {
            if (tipoPasta.isDirectory()) {
                lista.append(tipoPasta.getName()).append(":\n");
                for (File arquivo : tipoPasta.listFiles()) {
                    lista.append(" - ").append(arquivo.getName()).append("\n");
                }
            }
        }
        dos.writeUTF(lista.toString());
    }

    /**
     * Recebe um arquivo do cliente e salva na pasta correta do servidor.
     */
    private void receberArquivo(DataInputStream dis, String user) throws IOException {
        String tipo = dis.readUTF();              // Tipo do arquivo (pdf/jpg/txt)
        String nomeArquivo = dis.readUTF();       // Nome do arquivo
        long tamanho = dis.readLong();            // Tamanho do arquivo

        File destino = new File("armazenamento/" + user + "/" + tipo + "/" + nomeArquivo);
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            long restante = tamanho;
            int lido;

            while ((lido = dis.read(buffer, 0, (int)Math.min(buffer.length, restante))) > 0) {
                fos.write(buffer, 0, lido);
                restante -= lido;
                if (restante == 0) break;
            }
        }
    }

    /**
     * Envia um arquivo do servidor para o cliente, se o arquivo existir.
     */
    private void enviarArquivo(DataInputStream dis, DataOutputStream dos, String user) throws IOException {
        String tipo = dis.readUTF();              // Tipo do arquivo pedido
        String nomeArquivo = dis.readUTF();       // Nome do arquivo pedido
        File arquivo = new File("armazenamento/" + user + "/" + tipo + "/" + nomeArquivo);

        if (!arquivo.exists()) {
            dos.writeUTF("ARQUIVO_NAO_ENCONTRADO");
            return;
        }

        dos.writeUTF("ARQUIVO_OK");
        dos.writeLong(arquivo.length());

        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int lido;
            while ((lido = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, lido);
            }
        }
    }
}
