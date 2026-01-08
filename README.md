# Monday Builder (Montagsmaler)

**Monday Builder** is a fun, Pictionary-style minigame for Minecraft where players take turns building a secret word while others try to guess it.

## 🎮 How to Play

1.  **Start the Game**: A Game Master starts the session, defining how many rounds will be played.
2.  **The Builder**: Each round, one player is chosen as the **Builder**. They are given a secret word and must build it using blocks within a limited time.
3.  **The Guessers**: All other players are **Guessers**. They must watch the Builder and type their guesses into the chat.
4.  **Scoring**: 
    - **Guessers** earn points by being the first to guess the word correctly.
    - **Builders** earn points if their creation is guessed by others.
5.  **Winning**: After all rounds are completed, the player with the most points is crowned the winner!

## ✨ Key Features

-   **Difficulty Levels**: Choose between **Easy**, **Intermediate**, and **Strong** word categories to challenge your building skills.
-   **Real-time Feedback**: Notifications in the action bar keep you updated on the remaining time and game progress.
-   **Customizable**: Set your own player colors and manage game rounds easily.

## 🔊 Audio

-   **Volume Control**: You can adjust the game volume using the **MASTER** volume slider in your Minecraft sound settings.

## 🛠 Commands

-   `/mb start <rounds>`: Starts the game session. The Game Master defines the number of rounds to play.
-   `/mb pause`: Pauses the game, including the timer and current state, allowing for a break without losing progress.
-   `/mb resume`: Resumes the game from a paused state.
-   `/mb stop`: Ends the game immediately and teleports all players back to the lobby.
-   `/mb color <Player> <#HEX>`: Sets a player's specific building color using a HEX code.
-   `/mb role <Player> <ROLE>`: Manually changes a player's role (available when Debug mode is enabled in the config).

## 🎲 Mini Games

When not in a main game session, the Game Master can start various mini-games in the lobby:

### Tic Tac Toe
A classic 1v1 challenge played on a vertical wall within the Minecraft world.
-   **How to play**: Two players take turns interacting with the game board to place their symbols.
-   **Start command**: `/mb minigame tictactoe`

---
*Developed for Minecraft 1.21.10 using the Architectury framework.*
