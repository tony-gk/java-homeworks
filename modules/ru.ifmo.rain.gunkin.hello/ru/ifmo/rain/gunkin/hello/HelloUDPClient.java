package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    /**
     * Runs Hello client.
     *
     * @param host         server host
     * @param port         server port
     * @param prefix       request prefix
     * @param threadCount  number of request threads
     * @param requestCount number of requests per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int threadCount, int requestCount) {
        SocketAddress socketAddress = new InetSocketAddress(host, port);

        ExecutorService senderPool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            senderPool.submit(new RequestSender(socketAddress, prefix, requestCount, i));
        }

        senderPool.shutdown();
        try {
            senderPool.awaitTermination(threadCount * requestCount * 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class RequestSender implements Runnable {
        private static final int TIMEOUT = 1000;

        private final SocketAddress socketAddress;
        private final String prefix;
        private final int requestCount;
        private final int threadNumber;

        private RequestSender(SocketAddress socketAddress, String prefix, int requestCount, int threadNumber) {
            this.socketAddress = socketAddress;
            this.prefix = prefix;
            this.requestCount = requestCount;
            this.threadNumber = threadNumber;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(TIMEOUT);

                DatagramPacket responsePacket = createResponsePacket(socket);
                for (int i = 0; i < requestCount; i++) {
                    String request = prefix + threadNumber + "_" + i;
                    DatagramPacket requestPacket = createRequestPacket(request);

                    while (!socket.isClosed()) {
                        try {
                            socket.send(requestPacket);
                            System.out.println("Request: " + request);
                            socket.receive(responsePacket);

                            String response = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength(), StandardCharsets.UTF_8);
                            if (isValidResponse(response, request)) {
                                System.out.println("Response: " + response);
                                break;
                            }
                        } catch(SocketTimeoutException e) {
                            System.out.println("Timeout of receiving response has expired");
                        } catch (IOException e) {
                            System.out.println("I/O error occurred: " + e.getMessage());
                        }
                    }

                }
            } catch (SocketException e) {
                System.out.println("Error occurred during socket creation: " + e.getMessage());
            }
        }

        private boolean isValidResponse(String response, String request) {
            return response.length() != request.length() && response.contains(request);
        }

        private DatagramPacket createRequestPacket(String request) {
            byte[] requestBuffer = request.getBytes(StandardCharsets.UTF_8);
            return new DatagramPacket(requestBuffer, requestBuffer.length, socketAddress);
        }

        private DatagramPacket createResponsePacket(DatagramSocket socket) throws SocketException {
            byte[] responseBuffer;
            responseBuffer = new byte[socket.getReceiveBufferSize()];
            return new DatagramPacket(responseBuffer, responseBuffer.length);
        }
    }

}
