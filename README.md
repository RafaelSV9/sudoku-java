# 🧩 Sudoku em Java (Terminal)

Este é um jogo de **Sudoku interativo** implementado em **Java**, rodando diretamente no **terminal**.  
O projeto foi criado para praticar **Programação Orientada a Objetos (POO)**, manipulação de dados, uso de classes e métodos, além de lidar com **entradas e saídas** no console.

---

## 🚀 Funcionalidades
- Geração de tabuleiro **com solução única** (algoritmo de backtracking)
- **3 níveis de dificuldade**: Fácil, Médio e Difícil
- Validação de jogadas em tempo real
- Comandos interativos no terminal:
  - `play r c v` → insere valor **v** na posição (linha **r**, coluna **c**)
  - `erase r c` → apaga o valor de uma célula (se não for pista fixa)
  - `hint` → preenche automaticamente uma célula correta
  - `check` → verifica se há conflitos visíveis
  - `solve` → resolve o tabuleiro inteiro
  - `reset` → reinicia o puzzle para o estado inicial
  - `print` → mostra o tabuleiro atual
  - `help` → mostra os comandos disponíveis
  - `quit` → sai do jogo

---

## 📂 Estrutura do Projeto


sudoku-java/
│-- SudokuCLI.java # Código principal com todas as classes
│-- README.md # Este arquivo
│-- .gitignore # Arquivos ignorados pelo Git


---

## 🔧 Como compilar e executar

### Pré-requisitos
- **Java JDK 11+** instalado na sua máquina  
  Verifique com:
  ```bash
  java -version

Passos

Clone o repositório:

git clone https://github.com/SEU_USUARIO/sudoku-java.git
cd sudoku-java


Compile o código:

javac SudokuCLI.java


Execute o jogo:

java SudokuCLI

🕹️ Exemplo de uso
==== Sudoku (Terminal) ====
Escolha a dificuldade: [1] Fácil  [2] Médio  [3] Difícil
> 1

Dificuldade: EASY
Bom jogo!

> play 1 3 4
OK: (1,3) = 4

> check
Sem conflitos aparentes.

> hint
Dica: (2,5) = 7

📌 Próximos Passos

 Implementar salvamento e carregamento de jogos (save/load)

 Adicionar contagem de erros e cronômetro

 Criar uma versão com interface gráfica (Swing ou JavaFX)

 Escrever testes unitários (JUnit)

👨‍💻 Autor

Desenvolvido por Rafael dos Santos Vicente 🎸💻
Projeto para prática de Java + POO + Algoritmos.
