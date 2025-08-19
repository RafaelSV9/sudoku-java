import java.util.*;

/**
 * SudokuCLI.java
 * Jogo de Sudoku completo no terminal (Java 11+).
 *
 * Comandos durante o jogo:
 *  - play r c v   -> insere valor v (1..9) na linha r e coluna c (1..9)
 *  - erase r c    -> apaga valor em (r,c) se não for uma pista fixa
 *  - hint         -> coloca automaticamente um número correto em uma célula vazia
 *  - check        -> verifica se há erros visíveis
 *  - solve        -> resolve o tabuleiro inteiro
 *  - reset        -> volta ao estado inicial do puzzle
 *  - print        -> reimprime o tabuleiro
 *  - help         -> mostra ajuda
 *  - quit         -> sai do jogo
 */
public class SudokuCLI {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("==== Sudoku (Terminal) ====");
        System.out.println("Escolha a dificuldade: [1] Fácil  [2] Médio  [3] Difícil");
        int opt = readOption(sc, 1, 3);

        Difficulty diff = switch (opt) {
            case 1 -> Difficulty.EASY;
            case 2 -> Difficulty.MEDIUM;
            default -> Difficulty.HARD;
        };

        SudokuGame game = new SudokuGame(diff);
        game.loop(sc);
    }

    // Lê uma opção inteira entre min..max (com fallback simples)
    private static int readOption(Scanner sc, int min, int max) {
        while (true) {
            System.out.print("> ");
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (Exception ignored) {}
            System.out.printf("Digite um número entre %d e %d.%n", min, max);
        }
    }
}

/** Nível de dificuldade controla a quantidade de remoções. */
enum Difficulty {
    EASY(38), MEDIUM(46), HARD(54);
    final int removals;
    Difficulty(int removals) { this.removals = removals; }
}

/** Representa o tabuleiro de Sudoku 9x9. */
class Board implements Cloneable {
    private final int[][] cells = new int[9][9];
    private final boolean[][] fixed = new boolean[9][9]; // pistas iniciais

    public Board() {}

    public int get(int r, int c) { return cells[r][c]; }
    public void set(int r, int c, int v) { cells[r][c] = v; }
    public boolean isFixed(int r, int c) { return fixed[r][c]; }
    public void setFixed(int r, int c, boolean f) { fixed[r][c] = f; }

    public boolean isComplete() {
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++)
                if (cells[r][c] == 0) return false;
        return true;
    }

    public boolean isValidMove(int r, int c, int v) {
        if (v < 1 || v > 9) return false;
        // linha
        for (int cc=0; cc<9; cc++) if (cc!=c && cells[r][cc]==v) return false;
        // coluna
        for (int rr=0; rr<9; rr++) if (rr!=r && cells[rr][c]==v) return false;
        // bloco
        int br = (r/3)*3, bc = (c/3)*3;
        for (int rr=br; rr<br+3; rr++)
            for (int cc=bc; cc<bc+3; cc++)
                if (!(rr==r && cc==c) && cells[rr][cc]==v) return false;
        return true;
    }

    public void print() {
        System.out.println();
        System.out.println("    1 2 3   4 5 6   7 8 9");
        System.out.println("  +-------+-------+-------+");
        for (int r=0; r<9; r++) {
            System.out.print((r+1) + " | ");
            for (int c=0; c<9; c++) {
                int v = cells[r][c];
                String s = (v==0) ? "." : String.valueOf(v);
                System.out.print(s);
                System.out.print(((c%3)==2) ? " | " : " ");
            }
            System.out.println();
            if ((r%3)==2) System.out.println("  +-------+-------+-------+");
        }
        System.out.println();
    }

    /** Verifica se há conflitos visíveis (números duplicados) */
    public boolean hasVisibleConflicts() {
        // checa duplicações por posição preenchida
        for (int r=0;r<9;r++){
            for (int c=0;c<9;c++){
                int v = cells[r][c];
                if (v == 0) continue;
                cells[r][c] = 0; // remove temporariamente
                boolean ok = isValidMove(r,c,v);
                cells[r][c] = v;
                if (!ok) return true;
            }
        }
        return false;
    }

    @Override
    public Board clone() {
        Board b = new Board();
        for (int r=0;r<9;r++){
            System.arraycopy(this.cells[r], 0, b.cells[r], 0, 9);
            System.arraycopy(this.fixed[r], 0, b.fixed[r], 0, 9);
        }
        return b;
    }
}

/** Solver por backtracking + contagem de soluções (até 2). */
class Solver {
    public boolean solve(Board b) {
        int[] pos = findEmpty(b);
        if (pos == null) return true;
        int r = pos[0], c = pos[1];
        for (int v=1; v<=9; v++) {
            if (b.isValidMove(r,c,v)) {
                b.set(r,c,v);
                if (solve(b)) return true;
                b.set(r,c,0);
            }
        }
        return false;
    }

    public int countSolutions(Board b, int limit) {
        return count(b, limit);
    }

    private int count(Board b, int limit) {
        if (limit <= 0) return 0;
        int[] pos = findEmpty(b);
        if (pos == null) return 1;
        int r=pos[0], c=pos[1];
        int solutions = 0;
        for (int v=1; v<=9; v++) {
            if (b.isValidMove(r,c,v)) {
                b.set(r,c,v);
                solutions += count(b, limit - solutions);
                b.set(r,c,0);
                if (solutions >= limit) break;
            }
        }
        return solutions;
    }

    private int[] findEmpty(Board b) {
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++)
                if (b.get(r,c)==0) return new int[]{r,c};
        return null;
    }
}

/** Gerador de puzzles com solução única. */
class Generator {
    private final Random rnd = new Random();
    private final Solver solver = new Solver();

    public GeneratedPuzzle generate(Difficulty diff) {
        Board solved = makeFullSolution();
        Board puzzle = solved.clone();

        // lista de posições 0..80
        List<int[]> positions = new ArrayList<>();
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++)
                positions.add(new int[]{r,c});
        Collections.shuffle(positions, rnd);

        int removed = 0;
        for (int[] pos : positions) {
            if (removed >= diff.removals) break;
            int r=pos[0], c=pos[1];
            int backup = puzzle.get(r,c);
            puzzle.set(r,c,0);

            // checa unicidade
            Board test = puzzle.clone();
            int count = solver.countSolutions(test, 2);
            if (count != 1) {
                // voltar atrás
                puzzle.set(r,c,backup);
            } else {
                removed++;
            }
        }

        // marca pistas fixas
        Board initial = puzzle.clone();
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++)
                initial.setFixed(r,c, initial.get(r,c) != 0);

        return new GeneratedPuzzle(initial, solved);
    }

    /** Cria uma solução completa aleatória via backtracking com ordem embaralhada. */
    private Board makeFullSolution() {
        Board b = new Board();
        fill(b, 0);
        return b;
    }

    private boolean fill(Board b, int idx) {
        if (idx == 81) return true;
        int r = idx / 9, c = idx % 9;
        if (b.get(r,c) != 0) return fill(b, idx+1);

        List<Integer> vals = new ArrayList<>();
        for (int v=1; v<=9; v++) vals.add(v);
        Collections.shuffle(vals, rnd);
        for (int v : vals) {
            if (b.isValidMove(r,c,v)) {
                b.set(r,c,v);
                if (fill(b, idx+1)) return true;
                b.set(r,c,0);
            }
        }
        return false;
    }
}

class GeneratedPuzzle {
    final Board initial;
    final Board solution;
    GeneratedPuzzle(Board initial, Board solution) {
        this.initial = initial;
        this.solution = solution;
    }
}

/** Controla o loop do jogo e comandos do usuário. */
class SudokuGame {
    private final Generator generator = new Generator();
    private final Solver solver = new Solver();
    private final Difficulty difficulty;
    private Board initial;
    private Board current;
    private Board solution;

    SudokuGame(Difficulty difficulty) {
        this.difficulty = difficulty;
        GeneratedPuzzle gp = generator.generate(difficulty);
        this.initial = gp.initial;
        this.solution = gp.solution;
        this.current = initial.clone();
    }

    public void loop(Scanner sc) {
        printWelcome();
        current.print();
        printHelp();

        while (true) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase(Locale.ROOT);

            try {
                switch (cmd) {
                    case "play" -> {
                        if (parts.length != 4) { usagePlay(); break; }
                        int r = Integer.parseInt(parts[1]) - 1;
                        int c = Integer.parseInt(parts[2]) - 1;
                        int v = Integer.parseInt(parts[3]);
                        play(r,c,v);
                    }
                    case "erase" -> {
                        if (parts.length != 3) { System.out.println("Uso: erase r c"); break; }
                        int r = Integer.parseInt(parts[1]) - 1;
                        int c = Integer.parseInt(parts[2]) - 1;
                        erase(r,c);
                    }
                    case "hint" -> hint();
                    case "check" -> check();
                    case "solve" -> solveAll();
                    case "reset" -> reset();
                    case "print" -> current.print();
                    case "help" -> printHelp();
                    case "quit", "exit" -> { System.out.println("Até mais!"); return; }
                    default -> {
                        System.out.println("Comando desconhecido. Digite 'help' para ver a ajuda.");
                    }
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Parâmetros inválidos. Use números inteiros.");
            } catch (IndexOutOfBoundsException ioobe) {
                System.out.println("Coordenadas fora do intervalo (1..9).");
            }
        }
    }

    private void printWelcome() {
        System.out.println();
        System.out.println("Dificuldade: " + difficulty);
        System.out.println("Gereciado com solução única. Bom jogo!");
        System.out.println();
    }

    private void printHelp() {
        System.out.println("Comandos:");
        System.out.println("  play r c v   -> coloca valor v em (linha r, coluna c)");
        System.out.println("  erase r c    -> apaga valor em (r,c) se não for pista fixa");
        System.out.println("  hint         -> preenche uma célula vazia corretamente");
        System.out.println("  check        -> verifica conflitos visíveis");
        System.out.println("  solve        -> resolve todo o tabuleiro");
        System.out.println("  reset        -> volta ao início");
        System.out.println("  print        -> mostra o tabuleiro");
        System.out.println("  help         -> esta ajuda");
        System.out.println("  quit         -> sair");
        System.out.println();
    }

    private void usagePlay() {
        System.out.println("Uso: play r c v");
        System.out.println("Ex.: play 3 7 9  (linha 3, coluna 7, valor 9)");
    }

    private void play(int r, int c, int v) {
        ensureInside(r,c);
        if (current.isFixed(r,c)) {
            System.out.println("Essa posição é uma pista fixa. Não pode alterar.");
            return;
        }
        if (v < 1 || v > 9) {
            System.out.println("Valor deve ser entre 1 e 9.");
            return;
        }
        if (!current.isValidMove(r,c,v)) {
            System.out.println("Jogada inválida (conflita com linha/coluna/bloco).");
            return;
        }
        current.set(r,c,v);
        System.out.printf("OK: (%d,%d) = %d%n", r+1, c+1, v);
        current.print();

        if (current.isComplete() && !current.hasVisibleConflicts()) {
            System.out.println("Parabéns! Você completou o Sudoku.");
        }
    }

    private void erase(int r, int c) {
        ensureInside(r,c);
        if (current.isFixed(r,c)) {
            System.out.println("Essa posição é pista fixa. Não pode apagar.");
            return;
        }
        if (current.get(r,c) == 0) {
            System.out.println("Essa célula já está vazia.");
            return;
        }
        current.set(r,c,0);
        System.out.printf("Apagado: (%d,%d)%n", r+1, c+1);
        current.print();
    }

    private void hint() {
        // Procura uma célula vazia e usa a solução para preenchê-la
        for (int r=0;r<9;r++) {
            for (int c=0;c<9;c++) {
                if (current.get(r,c)==0) {
                    int v = solution.get(r,c);
                    current.set(r,c,v);
                    System.out.printf("Dica: (%d,%d) = %d%n", r+1, c+1, v);
                    current.print();
                    return;
                }
            }
        }
        System.out.println("Não há células vazias para dica.");
    }

    private void check() {
        boolean conflicts = current.hasVisibleConflicts();
        if (conflicts) {
            System.out.println("Há conflitos no tabuleiro (número repetido em linha/coluna/bloco).");
        } else {
            System.out.println("Sem conflitos aparentes.");
        }
    }

    private void solveAll() {
        Board temp = current.clone();
        if (solver.solve(temp)) {
            current = temp;
            // Manter as pistas iniciais marcadas
            for (int r=0;r<9;r++)
                for (int c=0;c<9;c++)
                    current.setFixed(r,c, initial.isFixed(r,c));
            System.out.println("Tabuleiro resolvido:");
            current.print();
        } else {
            System.out.println("Não foi possível resolver (inconsistente).");
        }
    }

    private void reset() {
        current = initial.clone();
        System.out.println("Puzzle resetado ao estado inicial.");
        current.print();
    }

    private void ensureInside(int r, int c) {
        if (r<0 || r>=9 || c<0 || c>=9) throw new IndexOutOfBoundsException();
    }
}
