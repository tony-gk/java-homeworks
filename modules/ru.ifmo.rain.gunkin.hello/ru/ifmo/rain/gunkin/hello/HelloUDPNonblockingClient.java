package ru.ifmo.rain.gunkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HelloUDPNonblockingClient implements HelloClient {
    private static final int TIMEOUT_MS = 600;

    /**
     * Runs Hello client.
     *
     * @param host         server host
     * @param port         server port
     * @param prefix       request prefix
     * @param channelCount number of request channels
     * @param requestCount number of requests per channel.
     */
    @Override
    public void run(String host, int port, String prefix, int channelCount, int requestCount) {
        InetSocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + host, e);
        }

        try {
            new RequestSender(prefix, channelCount, requestCount, socketAddress).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class RequestSender {
        private final static int BYTE_BUFFER_SIZE = 1024;

        private final String prefix;
        private final int channelCount;
        private final int requestCount;
        private final ByteBuffer buffer;
        private final List<ChannelContext> channelContexts;
        private final Selector selector;
        private final SocketAddress destination;

        public RequestSender(String prefix, int channelCount, int requestCount, SocketAddress destination) throws IOException {
            this.prefix = prefix;
            this.channelCount = channelCount;
            this.requestCount = requestCount;
            this.buffer = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
            this.channelContexts = new ArrayList<>();
            this.selector = Selector.open();
            this.destination = destination;

            for (int i = 0; i < channelCount; i++) {
                DatagramChannel datagramChannel = DatagramChannel.open();
                datagramChannel.configureBlocking(false);
                datagramChannel.register(selector, SelectionKey.OP_WRITE, i);

                channelContexts.add(new ChannelContext(datagramChannel, i));
            }
        }

        public void start() throws IOException {
            int finishedCount = 0;

            while (!Thread.interrupted() && finishedCount != channelCount) {
                selector.select(TIMEOUT_MS);

                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isWritable()) {
                        try {
                            sendRequest(key);
                        } catch (IOException e) {
                            System.err.println("I/O error occurred during sending");
                            e.printStackTrace();
                            continue;
                        }
                    }
                    if (key.isReadable()) {
                        try {
                            boolean isLastResponse = receiveResponse(key);
                            if (isLastResponse) {
                                finishedCount++;
                            }
                        } catch (IOException e) {
                            System.err.println("I/O error occurred during receiving");
                            e.printStackTrace();
                        }
                    }
                }
                checkForTimeout();
            }
            finish();
        }

        private void finish() throws IOException {
            for (ChannelContext channelContext : channelContexts) {
                channelContext.channel.close();
            }
            selector.close();
        }

        private void checkForTimeout() throws ClosedChannelException {
            for (ChannelContext channelContext : channelContexts) {
                if (channelContext.waitingForRead &&
                        System.currentTimeMillis() - channelContext.time > TIMEOUT_MS) {
                    channelContext.channel.register(selector, SelectionKey.OP_WRITE, channelContext.number);
                    channelContext.waitingForRead = false;
                }
            }
        }

        private boolean receiveResponse(SelectionKey key) throws IOException {
            ChannelContext channelContext = channelContexts.get((int) key.attachment());
            DatagramChannel channel = channelContext.channel;
            channelContext.waitingForRead = false;

            buffer.clear();
            channel.receive(buffer);

            buffer.flip();
            String response = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);

            if (ClientUtils.isValidEvilResponse(response, channelContext.number, channelContext.requestNumber)) {
                System.out.println("Response: " + response + " --- correct");
                channelContext.requestNumber++;
            } else {
                System.out.println("Response: " + response + " --- incorrect");
            }

            if (channelContext.requestNumber < requestCount) {
                channel.register(key.selector(), SelectionKey.OP_WRITE, channelContext.number);
                return false;
            } else {
                return true;
            }
        }

        private void sendRequest(SelectionKey key) throws IOException {
            ChannelContext channelContext = channelContexts.get((int) key.attachment());
            DatagramChannel channel = channelContext.channel;

            String request = prefix + channelContext.number + "_" + channelContext.requestNumber;

            buffer.clear();
            buffer.put(request.getBytes(StandardCharsets.UTF_8));
            buffer.flip();

            channel.send(buffer, destination);

            System.out.println("Request: " + request);
            channel.register(key.selector(), SelectionKey.OP_READ, channelContext.number);

            channelContext.waitingForRead = true;
            channelContext.time = System.currentTimeMillis();
        }
    }



    private static class ChannelContext {
        private final DatagramChannel channel;
        private final int number;
        private int requestNumber;
        private  long time;
        private boolean waitingForRead;

        private ChannelContext(DatagramChannel channel, int number) {
            this.channel = channel;
            this.number = number;
            this.requestNumber = 0;
            this.time = System.currentTimeMillis();
            this.waitingForRead = false;
        }
    }

    /**
     * Entry point into the application.
     *
     * @param args Usage: {@code <host> <port> <prefix> <threadCount> <requestCount>}
     */
    public static void main(String[] args) {
        ClientUtils.parseArgsAndRunClient(args, new HelloUDPNonblockingClient());
    }
}
