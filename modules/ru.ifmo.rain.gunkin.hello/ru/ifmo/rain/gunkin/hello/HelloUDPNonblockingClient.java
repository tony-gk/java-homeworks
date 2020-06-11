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

        Selector selector;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Failed to open selector");
            e.printStackTrace();
            return;
        }
        List<ChannelContext> channels = new ArrayList<>(channelCount);

        for (int i = 0; i < channelCount; i++) {
            try {
                DatagramChannel datagramChannel = DatagramChannel.open();
                datagramChannel.connect(socketAddress);
                datagramChannel.configureBlocking(false);
                datagramChannel.register(selector, SelectionKey.OP_WRITE, i);

                channels.add(new ChannelContext(datagramChannel, i));
            } catch (IOException e) {
                System.err.println("Failed to configure datagram channel");
                e.printStackTrace();
                return;
            }
        }

        try {
            startSelecting(selector, socketAddress, prefix, channelCount, requestCount, channels);
        } catch (IOException e) {
            System.err.println("IOException during selecting");
            e.printStackTrace();
        }
    }

    private void startSelecting(Selector selector, SocketAddress destination, String prefix,
                                int channelCount, int requestCount, List<ChannelContext> channels) throws IOException {
        int finishedCount = 0;
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (!Thread.interrupted() && finishedCount != channelCount) {
            selector.select(TIMEOUT_MS);

            for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                SelectionKey key = it.next();
                it.remove();

                if (key.isWritable()) {
                    try {
                        sendRequest(key, destination, prefix, buffer, channels);
                    } catch (IOException e) {
                        System.err.println("IOException during sending");
                        e.printStackTrace();
                        continue;
                    }
                }
                if (key.isReadable()) {
                    try {
                        boolean isLastResponse = receiveResponse(key, requestCount, buffer, channels);
                        if (isLastResponse) {
                            finishedCount++;
                        }
                    } catch (IOException e) {
                        System.err.println("IOException during receiving");
                        e.printStackTrace();
                    }
                }
            }

            for (ChannelContext channelContext : channels) {
                if (channelContext.isWaitingForRead &&
                        System.currentTimeMillis() - channelContext.time > TIMEOUT_MS) {
                    channelContext.channel.register(selector, SelectionKey.OP_WRITE, channelContext.number);
                    channelContext.isWaitingForRead = false;
                }
            }
        }
    }

    private boolean receiveResponse(SelectionKey key, int requestCount, ByteBuffer buffer, List<ChannelContext> channels) throws IOException {
        ChannelContext channelContext = channels.get((int) key.attachment());
        DatagramChannel channel = channelContext.channel;
        channelContext.isWaitingForRead = false;

        buffer.clear();
        channel.receive(buffer);

        buffer.flip();
        String response = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
        buffer.clear();

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

    private void sendRequest(SelectionKey key, SocketAddress destination, String prefix,
                             ByteBuffer buffer, List<ChannelContext> channels) throws IOException {
        ChannelContext channelContext = channels.get((int) key.attachment());
        DatagramChannel channel = channelContext.channel;

        String request = prefix + channelContext.number + "_" + channelContext.requestNumber;

        buffer.clear();
        buffer.put(request.getBytes(StandardCharsets.UTF_8));
        buffer.flip();

        channel.send(buffer, destination);

        System.out.println("Request: " + request);
        channel.register(key.selector(), SelectionKey.OP_READ, channelContext.number);

        channelContext.isWaitingForRead = true;
        channelContext.time = System.currentTimeMillis();
    }

    private static class ChannelContext {
        final DatagramChannel channel;
        final int number;
        int requestNumber;
        long time;
        boolean isWaitingForRead;

        ChannelContext(DatagramChannel channel, int number) {
            this.channel = channel;
            this.number = number;
            this.requestNumber = 0;
            this.time = System.currentTimeMillis();
            this.isWaitingForRead = false;
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
