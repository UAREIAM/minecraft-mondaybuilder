# Commands
```angular2html
/mb start <rounds>
/mb pause
/mb resume
/mb role <Player> <ROLE> (in DEBUG ONLY)
/mb stop
/mb color <Player> <#HEX>


/mb start <rounds> - starts the game (already exists)
/mb pause - can pause a game (pause the timer, pause the game state, keep ready for resuming at the current game state with the current time left)
/mb resume (resume from puase, does not take any effect if game state is not PAUSE)
/mb role <Player> <ROLE> (can change a role for the given user, needs DEBUG in config enabled)
/mb stop (stops the game at all, teleports all players to the lobby = this already should be the behaviour of stopping a game)
/mb color <Player> <#HEX> (can set the player's color to any HEX color - which is used as the player's color already does, changes only the color value)
```
# Build & Test
```angular2html
# compile
gradlew classes

# run
gradlew :fabric:runClient
gradlew :fabric:runServer
```
