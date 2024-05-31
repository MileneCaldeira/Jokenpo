package principal;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorJokenpo {
    private static ServerSocket servidorSocket;

    public static void main(String[] args) {
        int porta = obterPorta();
        

        try {
            servidorSocket = new ServerSocket(porta);
            System.out.println("Servidor Jokenpô iniciado na porta " + porta);

            while (true) {
                Socket clienteSocket = servidorSocket.accept();
                PrintWriter saida = new PrintWriter(clienteSocket.getOutputStream(), true);
                BufferedReader entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                saida.println("Escolha o modo de jogo: 1 - Jogador vs CPU, 2 - Jogador vs Jogador");
                String modo = entrada.readLine();


                if ("1".equals(modo)) {
                    new Thread(new Jogo(entrada, saida)).start();
                } else if ("2".equals(modo)) {
                    saida.println("Aguardando outro jogador...");
                    Socket jogador2Socket = servidorSocket.accept();
                    saida.println("Outro jogador conectado. Iniciando o jogo...");
                    (new PrintWriter(jogador2Socket.getOutputStream(),true)).println("Modo Jogador vs Jogador");
                    new Thread(new Jogo(clienteSocket, jogador2Socket)).start();
                } else {
                    saida.println("Modo inválido.");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static int obterPorta() {
        BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.println("Qual porta deseja usar?");
                return Integer.parseInt(leitor.readLine());
            } catch (IOException | NumberFormatException e) {
                System.out.println("Porta inválida. Tente novamente.");
            }
        }
    }
}


class Jogo implements Runnable {
    private Socket jogador1Socket;
    private Socket jogador2Socket;
    private PrintWriter jogador1Out;
    private PrintWriter jogador2Out;
    private BufferedReader jogador1In;
    private BufferedReader jogador2In;
    private BufferedReader entradaVsCpu;
    private PrintWriter saidaVsCpu;

    public Jogo(Socket jogador1Socket, Socket jogador2Socket) {
        this.jogador1Socket = jogador1Socket;
        this.jogador2Socket = jogador2Socket;
    }

    public Jogo(BufferedReader entrada, PrintWriter saida) {
        this.entradaVsCpu = entrada;
        this.saidaVsCpu = saida;

    }

    @Override
    public void run() {

        // Aqui faz a separação se é um jogo VsCpu ou JogadorxJogador

        if(this.entradaVsCpu != null && this.saidaVsCpu != null){

                int vitorias = 0;
                int derrotas = 0;
                int empates = 0;
                Random random = new Random();

                while (true) {
                    this.saidaVsCpu.println("Escolha: 1 - Pedra, 2 - Papel, 3 - Tesoura (ou 'sair' para terminar)");

                    String escolhaJogador = null;
                    try {
                        escolhaJogador = entradaVsCpu.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if ("sair".equalsIgnoreCase(escolhaJogador)) {
                        break;
                    }

                    int escolhaJogadorInt = Integer.parseInt(escolhaJogador);
                    int escolhaCPU = random.nextInt(3) + 1;
                    String[] opcoes = {"Pedra", "Papel", "Tesoura"};

                    saidaVsCpu.println("A CPU escolheu: " + opcoes[escolhaCPU - 1]);

                    int resultado = determinarVencedor(escolhaJogadorInt, escolhaCPU);

                    if (resultado == 0) {
                        empates++;
                        saidaVsCpu.println("Resultado: Empate!");
                    } else if (resultado == 1) {
                        vitorias++;
                        saidaVsCpu.println("Resultado: Você ganhou!");
                    } else {
                        derrotas++;
                        saidaVsCpu.println("Resultado: Você perdeu!");
                    }
                    saidaVsCpu.println("Vitórias: " + vitorias + " | Derrotas: " + derrotas + " | Empates: " + empates);
                }

        }else {


            try {
                jogador1In = new BufferedReader(new InputStreamReader(jogador1Socket.getInputStream()));
                jogador1Out = new PrintWriter(jogador1Socket.getOutputStream(), true);
                jogador2In = new BufferedReader(new InputStreamReader(jogador2Socket.getInputStream()));
                jogador2Out = new PrintWriter(jogador2Socket.getOutputStream(), true);

                // V,D,E
                //Ex: [[0,0,0],[0,0,0]] - Jogador 1 Indice 0 / Jogador 2 Indice 1
                int[][] placar = new int[2][3];


                while (true) {
                    jogador1Out.println("Sua vez jogador 1 - Escolha: 1 - Pedra, 2 - Papel, 3 - Tesoura (ou 'sair' para terminar)");
                    jogador2Out.println("Esperando o Jogador 1 jogar!");
                    String escolhaJogador1 = jogador1In.readLine();

                    if ("sair".equalsIgnoreCase(escolhaJogador1)) {
                        // desconeta os dois jogados;
                        jogador1Socket.close();
                        jogador2Out.println("O Jogador 1 saiu do jogo!");
                        jogador2Socket.close();
                        break;
                    }


                    jogador2Out.println("Sua vez jogador 2 - Escolha: 1 - Pedra, 2 - Papel, 3 - Tesoura (ou 'sair' para terminar)");
                    jogador1Out.println("Esperando o Jogador 2 jogar!");
                    String escolhaJogador2 = jogador2In.readLine();

                    if ("sair".equalsIgnoreCase(escolhaJogador2)) {
                        // desconeta os dois jogados;
                        jogador2Socket.close();
                        jogador1Out.println("O Jogador 2 saiu do jogo!");
                        jogador1Socket.close();
                        break;
                    }

                    int escolha1;
                    int escolha2;

                    try {
                        escolha1 = Integer.parseInt(escolhaJogador1);
                        escolha2 = Integer.parseInt(escolhaJogador2);
                    } catch (NumberFormatException e) {
                        jogador1Out.println("Escolha inválida. Tente novamente.");
                        jogador2Out.println("Escolha inválida. Tente novamente.");
                        continue;
                    }

                    int resultado = determinarVencedor(escolha1, escolha2);

                    String[] opcoes = {"Pedra", "Papel", "Tesoura"};

                    jogador1Out.println("O outro jogador escolheu: " + opcoes[escolha2 - 1]);
                    jogador2Out.println("O outro jogador escolheu: " + opcoes[escolha1 - 1]);

                    if (resultado == 0) {
                        jogador1Out.println("Resultado: Empate!");
                        placar[0][2] = 1;
                        placar[1][2] = 1;
                        jogador2Out.println("Resultado: Empate!");
                    } else if (resultado == 1) {
                        jogador1Out.println("Resultado: Você ganhou!");
                        jogador2Out.println("Resultado: Você perdeu!");
                        placar[0][0] = 1;
                        placar[1][1] = 1;
                    } else {
                        jogador1Out.println("Resultado: Você perdeu!");
                        jogador2Out.println("Resultado: Você ganhou!");
                        placar[1][0] = 1;
                        placar[0][1] = 1;
                    }

                    jogador1Out.println("Vitórias: " + placar[0][0] + " | Derrotas: " + placar[0][1] + " | Empates: " + placar[0][2]);
                    jogador2Out.println("Vitórias: " + placar[1][0] + " | Derrotas: " + placar[1][1] + " | Empates: " + placar[1][2]);


                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    jogador1Socket.close();
                    jogador2Socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int determinarVencedor(int jogador1, int jogador2) {
        if (jogador1 == jogador2) {
            return 0; // Empate
        } else if ((jogador1 == 1 && jogador2 == 3) || 
                   (jogador1 == 2 && jogador2 == 1) || 
                   (jogador1 == 3 && jogador2 == 2)) {
            return 1; // Jogador 1 vence
        } else {
            return 2; // Jogador 2 vence
        }
    }
}