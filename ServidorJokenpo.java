import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ServidorJokenpo {
    private static ServerSocket servidorSocket;

public static void main(String[] args) {
    int porta = obterPorta();

    try {
        servidorSocket = new ServerSocket(porta);
        System.out.println("Servidor Jokenpô iniciado na porta " + porta);

        List<Socket> jogadoresPendentes = new ArrayList<>();

        while (true) {
            Socket jogadorSocket = servidorSocket.accept();
            PrintWriter saidaJogador = new PrintWriter(jogadorSocket.getOutputStream(), true);
            BufferedReader entradaJogador = new BufferedReader(new InputStreamReader(jogadorSocket.getInputStream()));

            saidaJogador.println("Escolha o modo de jogo: 1 - Jogador vs CPU, 2 - Jogador vs Jogador");
            String modoJogador = entradaJogador.readLine();

            if ("1".equals(modoJogador)) {
                new Thread(() -> {
                    try {
                        jogadorVsCPU(entradaJogador, saidaJogador);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else if ("2".equals(modoJogador)) {
                jogadoresPendentes.add(jogadorSocket);

                if (jogadoresPendentes.size() >= 2) {
                    Socket jogador1Socket = jogadoresPendentes.remove(0);
                    Socket jogador2Socket = jogadoresPendentes.remove(0);

                    PrintWriter saidaJogador1 = new PrintWriter(jogador1Socket.getOutputStream(), true);
                    PrintWriter saidaJogador2 = new PrintWriter(jogador2Socket.getOutputStream(), true);

                    saidaJogador1.println("Outro jogador conectado. Iniciando o jogo...");
                    saidaJogador2.println("Outro jogador conectado. Iniciando o jogo...");

                    new Thread(new Jogo(jogador1Socket, jogador2Socket)).start();
                } else {
                    saidaJogador.println("Aguardando outro jogador...");
                }
            } else {
                saidaJogador.println("Modo inválido.");
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    

    private static void jogadorVsCPU(BufferedReader entrada, PrintWriter saida) throws IOException {
        int vitorias = 0;
        int derrotas = 0;
        int empates = 0;
        Random random = new Random();

        while (true) {
            saida.println("Escolha: 1 - Pedra, 2 - Papel, 3 - Tesoura (ou 'sair' para terminar)");
            String escolhaJogador = entrada.readLine();

            if ("sair".equalsIgnoreCase(escolhaJogador)) {
                break;
            }

            int escolhaJogadorInt;
            try {
                escolhaJogadorInt = Integer.parseInt(escolhaJogador);
            } catch (NumberFormatException e) {
                saida.println("Escolha inválida. Tente novamente.");
                continue;
            }

            int escolhaCPU = random.nextInt(3) + 1;
            String[] opcoes = {"Pedra", "Papel", "Tesoura"};

            saida.println("A CPU escolheu: " + opcoes[escolhaCPU - 1]);

            int resultado = determinarVencedor(escolhaJogadorInt, escolhaCPU);

            if (resultado == 0) {
                empates++;
                saida.println("Resultado: Empate!");
            } else if (resultado == 1) {
                vitorias++;
                saida.println("Resultado: Você ganhou!");
            } else {
                derrotas++;
                saida.println("Resultado: Você perdeu!");
            }
            saida.println("Vitórias: " + vitorias + " | Derrotas: " + derrotas + " | Empates: " + empates);
        }
    }

    private static int determinarVencedor(int jogador1, int jogador2) {
        if (jogador1 == jogador2) {
            return 0;
        } else if ((jogador1 == 1 && jogador2 == 3) ||
                (jogador1 == 2 && jogador2 == 1) ||
                (jogador1 == 3 && jogador2 == 2)) {
            return 1; 
        } else {
            return 2; 
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

    public Jogo(Socket jogador1Socket, Socket jogador2Socket) {
        this.jogador1Socket = jogador1Socket;
        this.jogador2Socket = jogador2Socket;
    }

    @Override
    public void run() {
        try {
            jogador1In = new BufferedReader(new InputStreamReader(jogador1Socket.getInputStream()));
            jogador1Out = new PrintWriter(jogador1Socket.getOutputStream(), true);
            jogador2In = new BufferedReader(new InputStreamReader(jogador2Socket.getInputStream()));
            jogador2Out = new PrintWriter(jogador2Socket.getOutputStream(), true);

            int[] placarJogador1 = {0, 0, 0}; // Vitórias Derrotas Empates
            int[] placarJogador2 = {0, 0, 0}; // Vitórias Derrotas Empates

            while (true) {
                jogador1Out.println("Sua vez jogador 1 - Escolha: 1 - Pedra, 2 - Papel, 3 - Tesoura (ou 'sair' para terminar)");
                jogador2Out.println("Esperando o Jogador 1 jogar!");
                String escolhaJogador1 = jogador1In.readLine();

                if ("sair".equalsIgnoreCase(escolhaJogador1)) {
                    jogador1Out.println("Você saiu do jogo.");
                    jogador2Out.println("O Jogador 1 saiu do jogo!");
                    break;
                }

                jogador2Out.println("Sua vez jogador 2 - Escolha: 1 - Pedra, 2 - Papel, 3 - Tesoura (ou 'sair' para terminar)");
                jogador1Out.println("Esperando o Jogador 2 jogar!");
                String escolhaJogador2 = jogador2In.readLine();

                if ("sair".equalsIgnoreCase(escolhaJogador2)) {
                    jogador2Out.println("Você saiu do jogo.");
                    jogador1Out.println("O Jogador 2 saiu do jogo!");
                    break;
                }
                    
                
                int escolhaJogador1Int;
                try {
                    escolhaJogador1Int = Integer.parseInt(escolhaJogador1);
                } catch (NumberFormatException e) {
                    jogador1Out.println("Escolha inválida. Tente novamente.");
                    continue;
                }

                int escolhaJogador2Int;
                try {
                    escolhaJogador2Int = Integer.parseInt(escolhaJogador2);
                } catch (NumberFormatException e) {
                    jogador2Out.println("Escolha inválida. Tente novamente.");
                    continue;
                }

                int resultado = determinarVencedor(escolhaJogador1Int, escolhaJogador2Int);

                if (resultado == 0) {
                    placarJogador1[2]++;
                    placarJogador2[2]++;
                    jogador1Out.println("Resultado: Empate!");
                    jogador2Out.println("Resultado: Empate!");
                } else if (resultado == 1) {
                    placarJogador1[0]++;
                    placarJogador2[1]++;
                    jogador1Out.println("Resultado: Jogador 1 ganhou!");
                    jogador2Out.println("Resultado: Jogador 1 ganhou!");
                } else {
                    placarJogador1[1]++;
                    placarJogador2[0]++;
                    jogador1Out.println("Resultado: Jogador 2 ganhou!");
                    jogador2Out.println("Resultado: Jogador 2 ganhou!");
                }

                jogador1Out.println("Placar - Vitórias: " + placarJogador1[0] + " | Derrotas: " + placarJogador1[1] + " | Empates: " + placarJogador1[2]);
                jogador2Out.println("Placar - Vitórias: " + placarJogador2[0] + " | Derrotas: " + placarJogador2[1] + " | Empates: " + placarJogador2[2]);
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

    private int determinarVencedor(int jogador1, int jogador2) {
        if (jogador1 == jogador2) {
            return 0;
        } else if ((jogador1 == 1 && jogador2 == 3) ||
                (jogador1 == 2 && jogador2 == 1) ||
                (jogador1 == 3 && jogador2 == 2)) {
            return 1; 
        } else {
            return 2; 
        }
    }
}
