package com.gt.lio.common.utils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AddressUtils {

    // 定义应用程序可以使用的端口范围
    private static final int MIN_APP_PORT = 1024; // 应用程序可使用的最小端口
    private static final int MAX_APP_PORT = 49151; // 应用程序可使用的最大端口（避免使用动态/私有端口）

    /**
     * 校验IP:PORT格式是否合法
     * @param address 待校验的地址（如"192.168.204.130:2181"）
     * @return 合法返回true，否则false
     */
    public static boolean isValidIpPort(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // 拆分IP和端口
        String[] parts = address.split(":");
        if (parts.length != 2) {
            return false;
        }

        String ip = parts[0];
        String portStr = parts[1];

        try {
            // 校验IP地址
            InetAddress inetAddress = InetAddress.getByName(ip);

            // 校验端口
            int port = Integer.parseInt(portStr);
            return port > 0 && port <= 65535;
        } catch (UnknownHostException e) {
            return false; // IP无效
        } catch (NumberFormatException e) {
            return false; // 端口不是数字
        }
    }

    public static boolean isValidIpPort(String ip, int port) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return isValidIpPort(ip + ":" + port);
    }

    /**
     * 获取服务器的第一个非回环 IPv4 地址。
     *
     * @return 第一个非回环 IPv4 地址，如果未找到则抛出 RuntimeException
     */
    public static String getServerIpAddress() {
        List<String> ipAddresses = getAllNonLoopbackIpAddresses();
        if (ipAddresses.isEmpty()) {
            throw new RuntimeException("No non-loopback IPv4 address found");
        }
        return ipAddresses.get(0); // 返回第一个非回环地址
    }

    /**
     * 获取所有非回环 IPv4 地址。
     *
     * @return 非回环 IPv4 地址列表（按网络接口顺序排序）
     */
    private static List<String> getAllNonLoopbackIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // 只获取 IPv4 地址（过滤掉 IPv6 和回环地址）
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
                        ipAddresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Failed to get network interfaces", e);
        }
        return ipAddresses;
    }


    /**
     * 校验端口是否合法：
     * 1. 端口不能为空且在 [MIN_APP_PORT, MAX_APP_PORT] 范围内
     * 2. 不应是系统保留端口
     *
     * @param port 端口号
     * @throws IllegalArgumentException 如果端口不合法
     */
    public static Integer validatePort(Integer port) {
        if (port == null) {
            throw new IllegalArgumentException("端口不能为空");
        }
        if (port < MIN_APP_PORT || port > MAX_APP_PORT) {
            throw new IllegalArgumentException(String.format("端口应在 %d 到 %d 之间，但当前值为 %d", MIN_APP_PORT, MAX_APP_PORT, port));
        }

        // 进一步检查端口是否已被绑定（这需要尝试打开该端口）
        if (isPortInUse(port)) {
            throw new IllegalArgumentException("端口 " + port + " 已被占用，请选择另一个端口。");
        }

        return port;
    }

    /**
     * 检查给定的端口是否已经被某个服务占用。
     *
     * @param port 需要检查的端口
     * @return 如果端口已被占用，则返回 true；否则返回 false
     */
    private static boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            // 尝试绑定端口
            socket.setReuseAddress(false);
            return false;
        } catch (IOException e) {
            // 绑定失败，说明端口已被占用
            return true;
        }
    }

}
