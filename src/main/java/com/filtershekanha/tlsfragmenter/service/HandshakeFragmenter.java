package com.filtershekanha.tlsfragmenter.service;

import com.filtershekanha.tlsfragmenter.config.AppConfig;
import com.filtershekanha.tlsfragmenter.utility.HexFormatter;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

@Service
public class HandshakeFragmenter {
    private static final Logger logger = LoggerFactory.getLogger(HandshakeFragmenter.class);
    private final AppConfig appConfig;

    public HandshakeFragmenter(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public boolean isClientHello(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            short recordType = TlsUtils.readUint8(inputStream);
            ProtocolVersion recordVersion = TlsUtils.readVersion(inputStream);
            int recordLength = TlsUtils.readUint16(inputStream);

            // Check if the content type is handshake (0x16)
            if (recordType != 0x16) {
                return false;
            }
            logger.info("Content Type is Handshake");

            logger.info("TLS Record Version: " + recordVersion.getName());
            logger.info("TLS Record Length: " + recordLength);

            byte[] handshakeData = new byte[recordLength];
            int bytesRead = inputStream.read(handshakeData);
            if (bytesRead != recordLength) {
                return false;
            }

            ByteArrayInputStream handshakeStream = new ByteArrayInputStream(handshakeData);
            short handshakeType = TlsUtils.readUint8(handshakeStream);

            // Check if the handshake type is client_hello (0x01)
            if (handshakeType != 0x01) {
                return false;
            }
            logger.info("*** ClientHello handshake detected ***");

            // Read and discard the handshake length
            TlsUtils.readUint24(handshakeStream);

            // Read and discard the client version
            ProtocolVersion protocolVersion = TlsUtils.readVersion(handshakeStream);
            logger.info("Handshake Version: " + protocolVersion.getName());

            // Read and discard random data
            byte[] random = new byte[32];
            TlsUtils.readFully(random, handshakeStream);

            // Read and discard session ID
            byte[] sessionID = TlsUtils.readOpaque8(handshakeStream);
            logger.info("Session ID: " + HexFormatter.toHexString(sessionID));

            // Read the cipher suites
            int cipherSuitesLength = TlsUtils.readUint16(handshakeStream);
            int[] cipherSuites = TlsUtils.readUint16Array(cipherSuitesLength / 2, handshakeStream); //  divided by 2 (since each cipher suite is 2 bytes long)
            logger.info("CipherSuites: " + HexFormatter.arrayToHexString(cipherSuites));

            // Read and discard compression methods
            int compressionMethodsLength = TlsUtils.readUint8(handshakeStream);
            short[] compressionMethods = TlsUtils.readUint8Array(compressionMethodsLength, handshakeStream);
            logger.info("CompressionMethods Length: " + compressionMethodsLength);
            logger.info("CompressionMethods: " + HexFormatter.arrayToHexString(compressionMethods));


            // Check if there are any extensions
            if (handshakeStream.available() > 0) {
                int extensionsLength = TlsUtils.readUint16(handshakeStream);
                byte[] extensionData = TlsUtils.readFully(extensionsLength, handshakeStream);

                ByteArrayInputStream extensionStream = new ByteArrayInputStream(extensionData);
                while (extensionStream.available() > 0) {
                    int extensionType = TlsUtils.readUint16(extensionStream);
                    int extensionLength = TlsUtils.readUint16(extensionStream);
                    byte[] extensionDataSingle = TlsUtils.readFully(extensionLength, extensionStream);

                    // Check if the extension type is SNI (0x00)
                    if (extensionType == 0x00) {
                        ByteArrayInputStream sniStream = new ByteArrayInputStream(extensionDataSingle);
                        int serverNameListLength = TlsUtils.readUint16(sniStream);
                        logger.info("Server Name List Length: " + serverNameListLength);

                        while (sniStream.available() > 0) {
                            short nameType = TlsUtils.readUint8(sniStream);
                            logger.info("SNI Type: " + nameType);
                            byte[] hostNameBytes = TlsUtils.readOpaque16(sniStream, 1);
                            String hostName = new String(hostNameBytes, StandardCharsets.UTF_8);
                            logger.info("SNI: " + hostName);
                        }
                    }

                    // Check if the extension type is ALPN (0x0010)
                    if (extensionType == 0x0010) {
                        ByteArrayInputStream alpnStream = new ByteArrayInputStream(extensionDataSingle);
                        int alpnListLength = TlsUtils.readUint16(alpnStream);
                        logger.info("ALPN List Length: " + alpnListLength);
                        List<String> alpnList = new ArrayList<>();

                        while (alpnStream.available() > 0) {
                            String protocol = new String(TlsUtils.readOpaque8(alpnStream, 1), StandardCharsets.UTF_8);
                            alpnList.add(protocol);
                        }

                        logger.info("ALPN List: " + String.join(", ", alpnList));
                    }

                    // Check if the extension type is supported_versions (0x002b)
                    if (extensionType == 0x002b) {
                        ByteArrayInputStream supportedVersionsStream = new ByteArrayInputStream(extensionDataSingle);
                        int supportedVersionsListLength = TlsUtils.readUint8(supportedVersionsStream);
                        List<ProtocolVersion> supportedVersionsList = new ArrayList<>();

                        for (int i = 0; i < supportedVersionsListLength / 2; i++) {
                            ProtocolVersion version = TlsUtils.readVersion(supportedVersionsStream);
                            supportedVersionsList.add(version);
                        }

                        logSupportedVersions(supportedVersionsList);

                    }
                }
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public void sendDataInFragments(byte[] data, int length, OutputStream out) throws IOException {
        int fragmentsSleep = appConfig.getFragmentsSleepMs();
        int totalFragments = appConfig.getFragmentsNumber();

        int remainingFragments = totalFragments;

        int totalSentBytes = 0;
        Random random = new Random();

        int minFragmentSize = Integer.MAX_VALUE;
        int maxFragmentSize = Integer.MIN_VALUE;

        logger.info("Initiating packet fragmentation...");
        while (totalSentBytes < length && remainingFragments > 0) {
            int remainingBytes = length - totalSentBytes;
            int avgFragmentSize = remainingBytes / remainingFragments;
            int fragmentSize;

            if (remainingFragments > 1) {
                fragmentSize = Math.min(avgFragmentSize + random.nextInt(avgFragmentSize) + 1, remainingBytes);
            } else {
                fragmentSize = remainingBytes;
            }

            // Ensure that we do not send more bytes than the packet size
            fragmentSize = Math.min(fragmentSize, length - totalSentBytes);

            minFragmentSize = Math.min(minFragmentSize, fragmentSize);
            maxFragmentSize = Math.max(maxFragmentSize, fragmentSize);

            out.write(data, totalSentBytes, fragmentSize);
            out.flush();

            int fragmentNumber = totalFragments - remainingFragments;
            logger.info(String.format(" -> Sending segment #%d [Offset: %d - %d]", fragmentNumber + 1, totalSentBytes, fragmentSize + totalSentBytes - 1));

            totalSentBytes += fragmentSize;
            remainingFragments--;

            try {
                Thread.sleep(fragmentsSleep);
            } catch (InterruptedException e) {
                logger.error("Error sleeping between fragments", e);
            }
        }

        logger.info("Packet fragmentation process has ended.");
        logger.info("Minimum fragment size: " + minFragmentSize);
        logger.info("Maximum fragment size: " + maxFragmentSize);
    }

    // ProtocolVersion does not recognize GREASE placeholders, so we implement a custom method to identify them.
    private boolean isGrease(ProtocolVersion version) {
        int major = version.getMajorVersion();
        int minor = version.getMinorVersion();
        return (major & 0x0F) == 0x0A && (minor & 0x0F) == 0x0A;
    }

    private void logSupportedVersions(List<ProtocolVersion> supportedVersionsList) {
        StringJoiner joiner = new StringJoiner(", ", "Supported Versions: ", "");
        for (ProtocolVersion version : supportedVersionsList) {
            String protocolName;
            if (isGrease(version)) {
                protocolName = "GREASE";
            } else {
                protocolName = version.getName();
            }
            joiner.add(protocolName);
        }
        logger.info(joiner.toString());
    }
}
