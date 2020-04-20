package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService requestHandlerPool;
    private Thread listener;
    private AtomicBoolean closed;


    /**
     * Starts a new Hello server.
     *
     * @param port        server port.
     * @param threadCount number of working threads.
     */
    @Override
    public void start(int port, int threadCount) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Failed to create socket: " + e.getMessage());
            return;
        }

        closed = new AtomicBoolean(false);
        requestHandlerPool = Executors.newFixedThreadPool(threadCount);
        listener = new Thread(this::listen);
        listener.start();
    }

    private void listen() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DatagramPacket requestPacket = createReceivePacket(socket);
                socket.receive(requestPacket);
                requestHandlerPool.submit(() -> handleRequest(requestPacket));
            } catch (IOException e) {
                if (!closed.get()) {
                    System.err.println("I/O error occurred during listening: " + e.getMessage());
                }
            }
        }
    }

    private void handleRequest(DatagramPacket requestPacket) {
        String request = getString(requestPacket);

        byte[] responseBuffer = ("Hello, " + request).getBytes(StandardCharsets.UTF_8);
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, requestPacket.getSocketAddress());

        try {
            socket.send(responsePacket);
        } catch (IOException e) {
            if (!closed.get()) {
                System.err.println("I/O error occurred during sending datagram: " + e.getMessage());
            }
        }
    }

    private String getString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    private DatagramPacket createReceivePacket(DatagramSocket socket) throws SocketException {
        byte[] responseBuffer;
        responseBuffer = new byte[socket.getReceiveBufferSize()];
        return new DatagramPacket(responseBuffer, responseBuffer.length);
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        closed.set(true);
        socket.close();
        listener.interrupt();
        requestHandlerPool.shutdownNow();

        try {
            requestHandlerPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

}
