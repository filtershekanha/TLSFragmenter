# TLS Handshake Fragmentation Proxy

This repository contains a Java implementation of a proxy server, built using the Spring Boot framework, that intercepts
and fragments the initial ClientHello message of a TLS handshake. The purpose of this proxy is to demonstrate a method
to bypass certain network security measures that may block or throttle specific types of encrypted traffic. By
leveraging the Spring Boot framework, the application benefits from a simplified development process, enhanced
scalability, and easy deployment.

## Overview

The proxy server listens on a local port and forwards incoming connections to a specified remote server. When the proxy
detects a ClientHello message in the upstream direction (from the client to the server), it breaks the message into
smaller fragments before forwarding them to the remote server. This fragmentation may help bypass certain security
measures that analyze packet sizes and content to identify specific types of traffic.

This Java-based proxy server is designed to fragment the initial TLS ClientHello message into smaller pieces, making it
more difficult for middleboxes, such as firewalls and intrusion detection systems, to identify and manipulate the
traffic. By breaking the ClientHello message into multiple fragments, it becomes challenging for these systems to
reassemble the original message, increasing the likelihood of secure, uninterrupted connections.

## How it Works

The proxy server listens for incoming connections from clients and forwards the traffic to the intended destination
server. The primary focus of this proxy is to manipulate the TLS ClientHello message, which is the first message
exchanged in a TLS handshake.

When the proxy receives a ClientHello message, it first verifies its validity. If the message is indeed a valid
ClientHello, the proxy proceeds to fragment the message into smaller pieces, with the number of fragments determined by
the `app.fragments-number` property. The fragments are then sent to the destination server with a brief delay, as
specified by the `app.fragments-sleep-ms` property, between each fragment. This approach ensures that the fragments are
transmitted with intended delays, increasing the difficulty of detecting and reassembling the original message.

Additionally, the proxy server takes advantage of the `TCP_NODELAY` socket option. This option, when enabled, ensures
that the local kernel sends TCP packets immediately, without any delay or buffering. By disabling Nagle's algorithm,
which normally buffers small packets and combines them into larger segments before sending them over the network, the
proxy maintains the intended delays between fragments, further increasing the complexity of detecting and reassembling
the original message.

## Configuration

The proxy server's behavior can be configured through the `application.properties` file, which contains the following
properties:

* `app.listen-port`: The local port on which the proxy server listens for incoming connections.
* `app.cloudflare-ip`: The remote server's IP address to which the proxy forwards the connections.
* `app.cloudflare-port`: The remote server's port to which the proxy forwards the connections.
* `app.fragments-number`: The number of fragments to split the ClientHello message into.
* `app.fragments-sleep-ms`: The delay (in milliseconds) between sending each fragment.
* `app.socket-timeout-ms`: The timeout (in milliseconds) for the proxy server's sockets.
* `app.buffer-size`: The size of the buffer (in bytes) used to read and write data between the client and the server.

You can modify these properties to customize the proxy server's behavior according to your needs.

## Prerequisites

To build and run the proxy, you need:

* JDK 1.8 or later
* Gradle 7.0 or later

## Building the Application

To build the application, follow these steps:

Open a terminal (Command Prompt for Windows or Terminal for Linux).
Navigate to the project's root directory, where the `build.gradle` file is located.
Run the following command:

```sh
gradle clean build
```

This command compiles the application and creates an executable JAR file in the `build/libs` directory.

## Running the Application

### Linux

To run the application on Linux, make sure the JAR file has executable permissions. If it does not, add them using the
following command:

```sh
chmod +x TLSFragmenter-<version>.jar
```

Now you can execute the JAR file directly:

```sh
./TLSFragmenter-<version>.jar
```

### Windows

To run the application on Windows, use the `java -jar` command:

```sh
java -jar TLSFragmenter-<version>.jar
```

The proxy server will start and listen on the specified port in the `application.properties` file.

## Changelog

[v1.1] 2023-04-30

* Moved logging of TLS record version and length inside the handshake condition, preventing incorrect messages from
  being logged when the packet is not a valid ClientHello
* Error logs have been simplified by removing exception stack traces, making logs less confusing for users

[v1.0] 2023-04-29

* Initial release

## License

[The MIT License (MIT)](https://raw.githubusercontent.com/filtershekanha/TLSFragmenter/master/LICENSE)

## Acknowledgments

This Java-based TLS handshake fragmentation proxy is inspired by and based on the Python
project [gfw_resist_tls_proxy](https://github.com/GFW-knocker/gfw_resist_tls_proxy). We appreciate the work of the
original authors and their contributions to the field. Please visit the original project's repository for more
information on its design and functionality.
