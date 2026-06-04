package com.example.cloudstudy.model;

import java.time.ZonedDateTime;

public class ServerStatus {
    private String hostName;
    private String privateIp;
    private String instanceId;
    private long usedMemoryBytes;
    private long maxMemoryBytes;
    private String systemCpuLoad;
    private String processCpuLoad;
    private ZonedDateTime currentTime;
    private String clientIp;
    private String xForwardedFor;
    private String xForwardedProto;
    private String xForwardedHost;
    private String host;
    private String userAgent;
    private String proxyOrLoadBalancerGuess;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public long getUsedMemoryBytes() {
        return usedMemoryBytes;
    }

    public void setUsedMemoryBytes(long usedMemoryBytes) {
        this.usedMemoryBytes = usedMemoryBytes;
    }

    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    public void setMaxMemoryBytes(long maxMemoryBytes) {
        this.maxMemoryBytes = maxMemoryBytes;
    }

    public String getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(String systemCpuLoad) {
        this.systemCpuLoad = systemCpuLoad;
    }

    public String getProcessCpuLoad() {
        return processCpuLoad;
    }

    public void setProcessCpuLoad(String processCpuLoad) {
        this.processCpuLoad = processCpuLoad;
    }

    public ZonedDateTime getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(ZonedDateTime currentTime) {
        this.currentTime = currentTime;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getXForwardedFor() {
        return xForwardedFor;
    }

    public void setXForwardedFor(String xForwardedFor) {
        this.xForwardedFor = xForwardedFor;
    }

    public String getXForwardedProto() {
        return xForwardedProto;
    }

    public void setXForwardedProto(String xForwardedProto) {
        this.xForwardedProto = xForwardedProto;
    }

    public String getXForwardedHost() {
        return xForwardedHost;
    }

    public void setXForwardedHost(String xForwardedHost) {
        this.xForwardedHost = xForwardedHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getProxyOrLoadBalancerGuess() {
        return proxyOrLoadBalancerGuess;
    }

    public void setProxyOrLoadBalancerGuess(String proxyOrLoadBalancerGuess) {
        this.proxyOrLoadBalancerGuess = proxyOrLoadBalancerGuess;
    }
}
