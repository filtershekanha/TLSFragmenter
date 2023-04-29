package com.filtershekanha.tlsfragmenter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public class AppConfig {
    private int listenPort;

    private String cloudflareIp;
    private int cloudflarePort;

    private int socketTimeoutMs;

    private int fragmentsNumber;
    private int fragmentsSleepMs;

    private int bufferSize;


    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getCloudflareIp() {
        return cloudflareIp;
    }

    public void setCloudflareIp(String cloudflareIp) {
        this.cloudflareIp = cloudflareIp;
    }

    public int getCloudflarePort() {
        return cloudflarePort;
    }

    public void setCloudflarePort(int cloudflarePort) {
        this.cloudflarePort = cloudflarePort;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs) {
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public int getFragmentsNumber() {
        return fragmentsNumber;
    }

    public void setFragmentsNumber(int fragmentsNumber) {
        this.fragmentsNumber = fragmentsNumber;
    }

    public int getFragmentsSleepMs() {
        return fragmentsSleepMs;
    }

    public void setFragmentsSleepMs(int fragmentsSleepMs) {
        this.fragmentsSleepMs = fragmentsSleepMs;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
