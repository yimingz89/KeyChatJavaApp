# KeyChat Java App Prototype 1

KeyChat (Prototype 1) is a Java Application where users exchange messages which are encrypted using the Keybase command line tool.  Download Keybase [here](https://keybase.io/download).

## How to run on Windows

Modify the `runclient.bat` and `runserver.bat` files so they have the correct parameters. 

Then you can run a KeyChat Server with the `runserver.bat` command and a KeyChat Client with the `runclient.bat` command.

## How to run on Linux

Modify the `runclient.sh` and `runserver.sh` files so they have the correct parameters. 

Then you can run a KeyChat Server with the `runserver.sh` command and a KeyChat Client with the `runclient.sh` command.

## Commands for the client

The client will be asked for a command. The commands available are: 

1. `list` - lists all users that are currently online
2. `send-message` - sends a message to another user via direct connection
3. `help-send-message` - sends a message to another user which is relayed through the main server
4. `exit` - exits the client program
