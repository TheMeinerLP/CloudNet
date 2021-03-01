package eu.cloudnetservice.cloudnet.v2.wrapper.server.process;

import eu.cloudnetservice.cloudnet.v2.lib.NetworkUtils;
import eu.cloudnetservice.cloudnet.v2.lib.server.ProxyProcessMeta;
import eu.cloudnetservice.cloudnet.v2.lib.server.ServerProcessMeta;
import eu.cloudnetservice.cloudnet.v2.wrapper.CloudNetWrapper;
import eu.cloudnetservice.cloudnet.v2.wrapper.server.BungeeCord;
import eu.cloudnetservice.cloudnet.v2.wrapper.server.GameServer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerProcessQueue implements Runnable {

    private final Queue<ServerProcessMeta> servers = new ConcurrentLinkedQueue<>();
    private final Queue<ProxyProcessMeta> proxies = new ConcurrentLinkedQueue<>();
    private final int processQueueSize;
    private volatile boolean running = true;

    public ServerProcessQueue(int processQueueSize) {
        this.processQueueSize = processQueueSize;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Queue<ServerProcessMeta> getServers() {
        return servers;
    }

    public int getProcessQueueSize() {
        return processQueueSize;
    }

    public Queue<ProxyProcessMeta> getProxies() {
        return proxies;
    }

    public void putProcess(ServerProcessMeta serverProcessMeta) {
        this.servers.offer(serverProcessMeta);
    }

    public void putProcess(ProxyProcessMeta proxyProcessMeta) {
        this.proxies.offer(proxyProcessMeta);
    }

    @Override
    public void run() {
        if (!Files.exists(Paths.get("local/spigot.jar"))) {
            setRunning(false);
            return;
        }
        if (!running) {
            return;
        }

        if (!servers.isEmpty() &&
            hasFreeCpu(CloudNetWrapper.getInstance().getWrapperConfig().getPercentOfCPUForANewServer())) {
            ServerProcessMeta serverProcessMeta = servers.poll();
            if (serverProcessMeta != null) {

                int memory = CloudNetWrapper.getInstance().getUsedMemory();

                if ((memory + serverProcessMeta.getMemory()) < CloudNetWrapper.getInstance().getMaxMemory()) {
                    try {
                        GameServer gameServer = new GameServer(
                            serverProcessMeta,
                            CloudNetWrapper.getInstance().getServerGroups().get(serverProcessMeta.getServiceId().getGroup()));

                        System.out.println("Fetching entry [" + gameServer.getServiceId() + ']');

                        if (!gameServer.bootstrap()) {
                            this.servers.add(serverProcessMeta);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.servers.add(serverProcessMeta);
                    }
                } else {
                    this.servers.add(serverProcessMeta);
                }
            }

        }

        if (!proxies.isEmpty() &&
            (hasFreeCpu(CloudNetWrapper.getInstance().getWrapperConfig().getPercentOfCPUForANewProxy()))) {

            ProxyProcessMeta proxyProcessMeta = proxies.poll();
            if (proxyProcessMeta != null) {
                int memory = CloudNetWrapper.getInstance().getUsedMemory();


                if ((memory + proxyProcessMeta.getMemory()) < CloudNetWrapper.getInstance().getMaxMemory()) {

                    BungeeCord bungeeCord = new BungeeCord(
                        proxyProcessMeta,
                        CloudNetWrapper.getInstance().getProxyGroups().get(proxyProcessMeta.getServiceId().getGroup()));

                    try {
                        System.out.println("Fetching entry [" + bungeeCord.getServiceId() + ']');

                        if (!bungeeCord.bootstrap()) {
                            this.proxies.add(proxyProcessMeta);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.proxies.add(proxyProcessMeta);
                    }
                } else {
                    this.proxies.add(proxyProcessMeta);
                }
            }
        }
    }

    private static boolean hasFreeCpu(final double percentOfCPUForANewServer) {
        return percentOfCPUForANewServer == 0.0D ||
            NetworkUtils.cpuUsage() <= percentOfCPUForANewServer;
    }

}
