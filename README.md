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