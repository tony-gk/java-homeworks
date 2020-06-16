package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HelloUDPNonblockingServer implements HelloServer {
    private final static int QUEUE_MAX_SIZE = 100_000;

    private Selector selector;
    private DatagramChannel serverChannel;
    private SelectionKey serverSelectionKey;
    private ExecutorService requestHandlerPool;
    private Thread selectorThread;

    private final AtomicBoolean closed = new AtomicBoolean(true);
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<Response>();

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

        requestHandlerPool = new ThreadPoolExecutor(threadCount, threadCount,
                0L, TimeUnit.MICROSECONDS,
                new LinkedBlockingQueue<>(QUEUE_MAX_SIZE), new ThreadPoolExecutor.DiscardPolicy());

        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Failed to open selector");
            e.printStackTrace();
            return;
        }

        try {
            serverChannel = DatagramChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverSelectionKey = serverChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("Failed to open and configure datagram channel");
            e.printStackTrace();
            return;
        }

        selectorThread = new Thread(this::runSelector);
        selectorThread.start();
    }

    private void runSelector() {
        while (!Thread.interrupted()) {
            try {
                selector.select();
                if (selector.selectedKeys().isEmpty()) {
                    continue;
                }
                selector.selectedKeys().clear();
            } catch (ClosedSelectorException e) {
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }


            try {
                if (serverSelectionKey.isReadable()) {
                    try {
                        receiveRequest();
                    } catch (IOException e) {
                        System.err.println("I/O error occurred during receiving");
                        e.printStackTrace();
                        continue;
                    }
                }

                if (serverSelectionKey.isWritable()) {
                    try {
                        sendResponse();
                    } catch (IOException e) {
                        System.err.println("I/O error occurred during sending");
                        e.printStackTrace();
                    }
                }
            } catch (CancelledKeyException ignored) {
            }
        }
    }

    private void receiveRequest() throws IOException {
        buffer.clear();
        SocketAddress srcAddress = serverChannel.receive(buffer);

        buffer.flip();
        String request = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);

        requestHandlerPool.submit(() -> handleRequest(request, srcAddress));
    }

    private void sendResponse() throws IOException {
        Response response;
        synchronized (responses) {
            response = responses.poll();
            if (responses.isEmpty()) {
                serverSelectionKey.interestOps(SelectionKey.OP_READ);
            }
        }
        Objects.requireNonNull(response);

        buffer.clear();
        buffer.put(response.message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();

        serverChannel.send(buffer, response.destination);
    }


    private void handleRequest(String request, SocketAddress srcAddress) {
        String responseMessage = "Hello, " + request;

        synchronized (responses) {
            if (responses.isEmpty()) {
                try {
                    serverSelectionKey.interestOpsOr(SelectionKey.OP_WRITE);
                    selector.wakeup();
                } catch (CancelledKeyException e) {
                    return;
                }
            }
            responses.add(new Response(responseMessage, srcAddress));
        }
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        closed.set(true);
        selectorThread.interrupt();
        try {
            selector.close();
        } catch (IOException e) {
            System.err.println("Failed to close server");
            e.printStackTrace();
        }

        try {
            serverChannel.close();
        } catch (IOException e) {
            System.err.println("Failed to close datagram channel");
            e.printStackTrace();
        }

        requestHandlerPool.shutdownNow();

        try {
            requestHandlerPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private static class Response {
        private final String message;
        private final SocketAddress destination;

        private Response(String message, SocketAddress destination) {
            this.message = message;
            this.destination = destination;
        }

    }

    /**
     * Entry point into the application.
     *
     * @param args Usage: {@code <port> <threadCount>}
     */
    public static void main(String[] args) {
        ServerUtils.parseArgsAndStartServer(args, new HelloUDPNonblockingServer());
    }
}
