package com.example.cloudstudy.service;

import com.example.cloudstudy.model.ServerStatus;

import javax.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Collections;

public class ServerStatusService {
    public ServerStatus getStatus(HttpServletRequest request) {
        ServerStatus status = new ServerStatus();
        Runtime runtime = Runtime.getRuntime();

        String hostName = resolveHostName();
        String xForwardedFor = header(request, "X-Forwarded-For");

        status.setHostName(hostName);
        status.setPrivateIp(resolvePrivateIpv4());
        status.setInstanceId(resolveInstanceId(hostName));
        status.setUsedMemoryBytes(runtime.totalMemory() - runtime.freeMemory());
        status.setMaxMemoryBytes(runtime.maxMemory());
        status.setSystemCpuLoad(cpuLoad("system"));
        status.setProcessCpuLoad(cpuLoad("process"));
        status.setCurrentTime(ZonedDateTime.now());
        status.setClientIp(request.getRemoteAddr());
        status.setXForwardedFor(xForwardedFor);
        status.setXForwardedProto(header(request, "X-Forwarded-Proto"));
        status.setXForwardedHost(header(request, "X-Forwarded-Host"));
        status.setHost(header(request, "Host"));
        status.setUserAgent(header(request, "User-Agent"));
        status.setProxyOrLoadBalancerGuess(xForwardedFor == null || xForwardedFor.isEmpty()
                ? "직접 요청 또는 X-Forwarded-For 헤더 미전달 상태로 추정"
                : "프록시 또는 로드밸런서를 거친 요청으로 추정");

        return status;
    }

    private String resolveInstanceId(String hostName) {
        String fromEnv = trimToNull(System.getenv("INSTANCE_ID"));
        if (fromEnv != null) {
            return fromEnv;
        }

        String fromProperty = trimToNull(System.getProperty("instance.id"));
        if (fromProperty != null) {
            return fromProperty;
        }

        return hostName;
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }

    private String resolvePrivateIpv4() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            return "N/A";
        }

        return "N/A";
    }

    private String cpuLoad(String type) {
        java.lang.management.OperatingSystemMXBean standardBean = ManagementFactory.getOperatingSystemMXBean();
        if (!(standardBean instanceof com.sun.management.OperatingSystemMXBean)) {
            return "N/A";
        }

        com.sun.management.OperatingSystemMXBean bean =
                (com.sun.management.OperatingSystemMXBean) standardBean;
        double value = "system".equals(type) ? bean.getSystemCpuLoad() : bean.getProcessCpuLoad();

        if (value < 0) {
            return "N/A";
        }

        return String.format("%.2f%%", value * 100);
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
