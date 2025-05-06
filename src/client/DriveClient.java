package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Cliente que se conecta ao servidor para listar, enviar e baixar arquivos.
 * A conexão usa o protocolo TCP para manter a comunicação estável.
 */
public class DriveClient {

    private static final String SERVER_IP = "localhost";  // IP do servidor (local)
    private static final int SERVER_PORT = 12345;         // Porta usada pelo servidor

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT); // Faz a conexão com o servidor
            DataInputStream dis = new DataInputStream(socket.getInputStream()); // Recebe dados
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // Envia dados
            Scanner scanner = new Scanner(System.in) // Leitor para entrada de dados pelo teclado
        ) {
            // Primeiro passo: realizar login
            realizarLogin(dis, dos, scanner);

            // Depois que logar, abre o menu de ações
            executarMenu(dis, dos, scanner);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Método que realiza o processo de login.
     * O cliente digita usuário e senha que são enviados para o servidor.
     */
    private static void realizarLogin(DataInputStream dis, DataOutputStream dos, Scanner scanner) throws IOException {
        System.out.println(dis.readUTF());  // Mensagem: "Usuario:"
        String username = scanner.nextLine();  // Usuário
        dos.writeUTF(username);

        System.out.println(dis.readUTF());  // Mensagem: "Senha:"
        String password = scanner.nextLine();  // Senha
        dos.writeUTF(password);

        String resposta = dis.readUTF();  // Resposta do servidor

        if (!resposta.equals("LOGIN_SUCESSO")) {
            System.out.println("Login falhou!");
            System.exit(0);  // Fecha o programa se a senha estiver errada
        } else {
            System.out.println("Login realizado com sucesso!");
        }
    }

    /**
     * Exibe o menu para o usuário escolher o que quer fazer:
     * listar, enviar, baixar ou sair.
     */
    private static void executarMenu(DataInputStream dis, DataOutputStream dos, Scanner scanner) throws IOException {
        boolean ativo = true;

        while (ativo) {
            System.out.println("\n1- Listar arquivos\n2- Fazer upload\n3- Fazer download\n4- Sair");
            int escolha = scanner.nextInt();
            scanner.nextLine();  // Limpar o ENTER que sobra

            switch (escolha) {
                case 1:
                    listarArquivos(dos, dis);
                    break;
                case 2:
                    fazerUpload(dos, scanner);
                    break;
                case 3:
                    fazerDownload(dis, dos, scanner);
                    break;
                case 4:
                    dos.writeUTF("EXIT");
                    ativo = false;
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        }
    }

    /**
     * Envia o comando "LIST" para o servidor
     * e exibe a lista de arquivos recebida.
     */
    private static void listarArquivos(DataOutputStream dos, DataInputStream dis) throws IOException {
        dos.writeUTF("LIST");
        String lista = dis.readUTF();
        System.out.println("\nArquivos disponíveis:\n" + lista);
    }

    /**
     * Realiza o envio de um arquivo do cliente para o servidor.
     * Envia nome, tipo e conteúdo do arquivo.
     */
    private static void fazerUpload(DataOutputStream dos, Scanner scanner) throws IOException {
        System.out.print("Informe o tipo do arquivo (pdf/jpg/txt): ");
        String tipo = scanner.nextLine();

        System.out.print("Informe o caminho do arquivo: ");
        String caminho = scanner.nextLine();
        File arquivo = new File(caminho);

        if (!arquivo.exists()) {
            System.out.println("Arquivo não encontrado!");
            return;
        }

        dos.writeUTF("UPLOAD");
        dos.writeUTF(tipo);
        dos.writeUTF(arquivo.getName());
        dos.writeLong(arquivo.length());

        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int lido;
            while ((lido = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, lido);
            }
        }

        System.out.println("Arquivo enviado com sucesso!");
    }

    /**
     * Solicita o download de um arquivo ao servidor.
     * Se o arquivo existir, salva localmente com o prefixo "downloaded_".
     */
    private static void fazerDownload(DataInputStream dis, DataOutputStream dos, Scanner scanner) throws IOException {
        System.out.print("Digite o tipo do arquivo (pdf/jpg/txt): ");
        String tipo = scanner.nextLine();

        System.out.print("Digite o nome do arquivo: ");
        String nome = scanner.nextLine();

        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(tipo);
        dos.writeUTF(nome);

        String status = dis.readUTF();

        if (status.equals("ARQUIVO_OK")) {
            long tamanho = dis.readLong();
            File destino = new File("downloaded_" + nome);

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                byte[] buffer = new byte[4096];
                int lido;
                long restante = tamanho;

                while ((lido = dis.read(buffer, 0, (int)Math.min(buffer.length, restante))) > 0) {
                    fos.write(buffer, 0, lido);
                    restante -= lido;
                    if (restante == 0) break;
                }
            }

            System.out.println("Arquivo baixado com sucesso!");

        } else {
            System.out.println("Arquivo não encontrado no servidor!");
        }
    }
}


// "C:\Users\nasci\Downloads\OAT.pdf"
// "C:\Users\nasci\Downloads\OAT.txt"
// "C:\Users\nasci\Downloads\OAT.pdf"
// "C:\Users\nasci\Downloads\OAT.txt"