package com.filtershekanha.tlsfragmenter.service;

import com.filtershekanha.tlsfragmenter.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final AppConfig appConfig;
    private final HandshakeFragmenter handshakeFragmenter;

    public Server(AppConfig appConfig, HandshakeFragmenter handshakeFragmenter) {
        AtomicBoolean interruption = new AtomicBoolean(false);

        this.appConfig = appConfig;
        this.handshakeFragmenter = handshakeFragmenter;

        int listenPort = appConfig.getListenPort();

        logger.info("Now listening at: 127.0.0.1:" + listenPort);

        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {

            // Add shutdown hook to handle graceful shutdown (e.g., Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    interruption.set(true);
                    serverSocket.close();
                } catch (IOException ignore) {
                }
            }));


            // Break the loop when the interruption flag is set
            while (!interruption.get()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            if (interruption.get()) {
                logger.info("Process interrupted");
            } else {
                logger.error("Error in main server loop", e);
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        String cloudflareIp = appConfig.getCloudflareIp();
        int cloudflarePort = appConfig.getCloudflarePort();
        int socketTimeout = appConfig.getSocketTimeoutMs();

        try {
            clientSocket.setSoTimeout(socketTimeout);

            /*
             * try-with-resources will automatically close the Socket at the end of the try block. In this case,
             * we want the sockets to remain open while the threads are running, so you should not use
             * try-with-resources here.
             */
            @SuppressWarnings("resource") final Socket backendSocket = new Socket(cloudflareIp, cloudflarePort);
            backendSocket.setSoTimeout(socketTimeout);
            backendSocket.setTcpNoDelay(true); // disables Nagle's algorithm


            executorService.submit(() -> copyData(clientSocket, backendSocket, true));
            executorService.submit(() -> copyData(backendSocket, clientSocket, false));
        } catch (IOException e) {
            try {
                clientSocket.close();
            } catch (IOException closeException) {
                logger.error("Error closing client socket", closeException);
            }
        }
    }

    private void copyData(Socket src, Socket dest, boolean isUpstream) {
        try (InputStream in = src.getInputStream();
             OutputStream out = dest.getOutputStream()) {

            long startTime = System.currentTimeMillis();

            byte[] buffer = new byte[appConfig.getBufferSize()];
            int bytesRead;

            if (isUpstream) {
                bytesRead = in.read(buffer);
                if (bytesRead > 0) {
                    boolean isClientHello = handshakeFragmenter.isClientHello(buffer);
                    if (isClientHello) {
                        handshakeFragmenter.sendDataInFragments(buffer, bytesRead, out);
                        logger.info(String.format("The process took %d milliseconds.", System.currentTimeMillis() - startTime));
                    } else {
                        logger.warn("Received packet is not a ClientHello message");
                    }
                }
            }

            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (SocketTimeoutException e) {
            logger.warn("Socket read timed out", e);
        } catch (SocketException e) {
            logger.info("Socket closed");
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("Error copying data", e);
        } finally {
            try {
                src.close();
                dest.close();
            } catch (IOException e) {
                logger.error("Error closing sockets", e);
            }
        }
    }

}
