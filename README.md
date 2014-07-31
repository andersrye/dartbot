# dartbot

Dartbot for [mad-darts](https://github.com/andersrye/mad-darts "mad-darts")

## Running dartbot

* Install [Leiningen](http://leiningen.org)
* Clone this repo
* Navigate to dartbot folder and run using one of the following commands:  


		lein run

or

		lein trampoline run

## Communicating with dartbot

Communicating with the dartbot is done either by broadcasting UDP messages or over websockets.

Connect using websockets at:

    ws://<ip>:8080/dartbot
    
If you don't know the IP, broadcast the following message on port 5000

    hello?

and the bot will reply with its IP on the same port.

After connecting, you will need to send an initial request message, in order to start receiving. Messages over websockets are JSON-strings.

    {"command" : "request", "get" : "all", "update" : "all"}

The "get" parameter defines what initial data is requested, and has the following options:
* __"all"__ - All current games will be returned.
* __"[gid123]"__ - Returns the game with the given ID.
* __"none"__ or option missing - no games returned.

The "update" parameter defeines what type of updates the client receives, and has the following options:
* __"all"__ - All full games are re-sent on each update.
* __"diff"__ - Only the difference between the old state and the new is sent on each update, has to be merged with the previous state client-side.
* __"messages"__ - messages sent to the robot are relayed to clients in this mode.
* __"none"__ or option missing - no updates are sent and the connection is closed.

## Messages
All of these messages can be sent to the bot to control it. The JSON/websocket messages are also sent to clients who have requested message updates.

### Start

Send to the bot to start a new game.

#### JSON/Websockets
Example:

    {"command" : "start", "gid" : "gid1", "payload" : {"timestamp" : 1391449631516, "bid" : "bid1", "rule2 : "301", "players" : ["bno" "yns" "hen"]}}
#### UDP

    START;TIMESTAMP;GAME-ID;BOARD-ID;GAME-MODE;PLAYER-ID,PLAYER-ID,...,PLAYER-ID, PLAYER-ID
Example:

    START;1391449631516;GID1;BID1;301;BNO,YNS,HEN

### Throw
Send to the bot to register a throw from a given board. 


#### JSON/Websockets
Example:

    {"command" : "throw", "bid" : "bid1", "payload" : {"timestamp" : 1393701549, "score" : 20, "multiplier" : 1}}

#### UDP
    
    THROW;TIMESTAMP;BOARD-ID;SEGMENT;MULTIPLIER
Example:

    THROW;1391449631516;BID1;20;3


## Next
Send to the bot to change the current player

#### JSON/Websockets
To let the bot select the next player (next player that has not finished):

    {"command" : "next", "gid" : "gid1393572367", "payload" : {"timestamp" : 1391449631516}}
To select a specific player:

    {"command" : "next", "gid" : "gid1393572367", "payload" : {"timestamp" : 1391449631516, "player" : "ary"}}
    
#### UDP
To let the bot select the next player (next player that has not finished):

    //NEXT;TIMESTAMP;GAME-ID
    
    NEXT;1391449631516;GID1
To select a specific player:

    //NEXT;TIMESTAMP;GAME-ID;PLAYER-ID
    
    NEXT;1391449631516;GID1;HEN    

## Delete
Send to the bot to delete the specified game.

#### JSON/Websockets

    {"command" : "delete", "gid" : "gid1393572367"}

#### UDP

    DELETE;GID1393572367
    
## End
Send to the bot to store the game (database + to file, locally) and then remove the game.

#### JSON/Websockets
    {"command" : "end", "gid" : "gid1393572367"}
#### UDP
    END;GID1393572367
## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

mvn deploy:deploy-file -DgroupId=local -DartifactId=SerialComm -Dversion=0.9 -Dpackaging=jar -Dfile=SerialComm.jar -Durl=file:lib
