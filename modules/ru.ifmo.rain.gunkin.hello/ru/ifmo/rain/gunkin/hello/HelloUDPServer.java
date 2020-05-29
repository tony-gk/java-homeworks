package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HelloUDPServer implements HelloServer {
    private final static int QUEUE_MAX_SIZE = 100_000;

    private DatagramSocket socket;
    private ExecutorService requestHandlerPool;
    private Thread receiver;
    private final AtomicBoolean closed = new AtomicBoolean(true);

    /**
     * Starts a new Hello server.
     *
     * @param port        server port.
     * @param threadCount number of working threads.
     */
    @Override
    public void start(int port, int threadCount) {
        if (!closed.compareAndSet(true, false)) {
            throw new IllegalArgumentException("Server is already running");
        }

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            closed.set(true);
            throw new IllegalArgumentException("Failed to create socket", e);
        }

        requestHandlerPool = new ThreadPoolExecutor(threadCount, threadCount,
                0L, TimeUnit.MICROSECONDS,
                new LinkedBlockingQueue<>(QUEUE_MAX_SIZE), new ThreadPoolExecutor.DiscardPolicy());

        receiver = new Thread(this::receivePackets);
        receiver.start();
    }

    private void receivePackets() {
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
        receiver.interrupt();
        requestHandlerPool.shutdownNow();

        try {
            requestHandlerPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }


    /**
     * Entry point into the application.
     *
     * @param args Usage: {@code <port> <threadCount>}
     */
    public static void main(String[] args) {
        Objects.requireNonNull(args, "Arguments array is null");
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected 2 arguments");
        }

        for (int i = 0; i < args.length; i++) {
            Objects.requireNonNull(args[i], "Argument " + i + " is null");
        }

        int port = parseArgument(args[0], "port");
        int threadCount = parseArgument(args[0], "count of threads");
        new HelloUDPServer().start(port, threadCount);
    }

    private static int parseArgument(String arg, String name) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Expected integer " + name);
        }
    }
}
