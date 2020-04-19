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
            int finalIndex = i;
            senderPool.submit(() ->
                    sendRequests(socketAddress, prefix, requestCount, finalIndex));
        }
        senderPool.shutdown();
        try {
            senderPool.awaitTermination(threadCount * requestCount * 1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendRequests(SocketAddress socketAddress, String prefix, int requestCount, int threadNumber) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1000);
            byte[] responseBuffer;
            responseBuffer = new byte[socket.getReceiveBufferSize()];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            for (int i = 0; i < requestCount; i++) {
                String request = prefix + threadNumber + "_" + i;
                byte[] requestBuffer = request.getBytes(StandardCharsets.UTF_8);
                DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length, socketAddress);

                while (!socket.isClosed()) {
                    System.out.println("Request: " + request);
                    try {
                        socket.send(requestPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    try {
                        socket.receive(responsePacket);
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    String response = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength(), StandardCharsets.UTF_8);
                    if (response.length() != request.length() &&
                            response.contains(request)) {
                        System.out.println("Response: " + response);
                        break;
                    } else {
                        System.out.println("Bad response: " + response);
                    }
                }

            }
        } catch (
                SocketException e) {
            e.printStackTrace();
        }
    }

}
