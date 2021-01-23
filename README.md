# Decentralized Chat Software

## Technical Design

This project aims to create secure and portable decentralized chat software.
The client is written with Swing. Client-server interaction is done via
gRPC, which will make it trivial to implement clients or servers in other
languages in the future. RabbitMQ is used internally to dispatch messages to 
connected clients, and MongoDB is used for persistent storage.

The authentication system is unique in that it does not require passwords or
tokens to be shared between servers. Instead, each user will an authentication
server with which they are identified, which will provide that users public
RSA key upon request. All messages to the server are signed, and verified
with the public key provided by the user's authentication server.

Servers may choose to implement password based authentication and store keys
server-side, but this is not required. The current implementation assumes
that the user will store his or her own private key.

### Components

The authentication server is very simple and only needs to implement one RPC
method, so it should be trivial for privacy and security conscious users to
run their own authentication server to avoid having any other party in control.
The authentication server is somewhat of a single point of failure in that
if it is hacked or controlled by a malicious party, users registered by the
authentication server could be impersonated by replacing their public keys.
For this reason there will be a way to specify upon first connection to a chat
server that the user's public key will never change.

The chat server handles communication between connected users in a pretty
standard way.

> Note: the chat server knows a "user" just as a user name, and the domain of
> the authentication server, e.g: bob@authserver.example.com
>
> Chat servers verify that the sender is actually bob by verifying bob's
> messages against the public key provided by authserver.example.com

## Usage

Each chat server has many groups. Each group can have many channels (like
slack channels) and many users. Users can participate in many groups, in many
chat servers at the same time, under the same identity.

# CI binaries

Binaries (jars) are available for each commit via. sourcehut builds
[here](https://builds.sr.ht/~swisschili?search=chat). You need Java 8 or later,
but I recommend using OpenJDK instead of the Oracle JVM. You can install it
on Debian based systems with `apt install openjdk-11-jre`.

# Public development group

![Joining public development group](join.png)

Anyone can create an account on the `chat.swisschili.sh` server.
Right click the left panel and join the `dev`
group on `chat.swisschili.sh` once you have made an account to discuss the
software. The server will go down from time to time, and since the API is not
yet stable make sure you are always using the most recent client from the CI.

Note that data may be wiped off the server at any time (although I'll try not to),
so if your messages are failing to send with a signature error (check stderr)
try logging out and registering for a new account.

# Building from source

```sh
# Build an executable jar
$ ./gradlew shadowJar
# Only build the client
$ ./gradlew :client:shadowJar
# Build and run tests (requires MongoDB)
$ ./gradlew build -Ddb-url=mongodb://user:password@host:27017/database
```

# License

```
Decentralized chat software
Copyright (C) 2021  swissChili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
