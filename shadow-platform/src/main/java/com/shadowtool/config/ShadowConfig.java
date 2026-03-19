package com.shadowtool.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shadow")
@Getter
@Setter
public class ShadowConfig {

    /** Master on/off switch for shadow mirroring */
    private boolean enabled = true;

    /** 0.0 to 1.0 — fraction of requests to mirror */
    private double samplingRate = 1.0;

    /** v1 primary service base URL */
    private String v1BaseUrl;

    /** v2 shadow service base URL */
    private String v2BaseUrl;

    /** Timeout in ms for v2 shadow calls */
    private long v2TimeoutMs = 5000;

    public boolean shouldMirror() {
        return enabled && Math.random() < samplingRate;
    }
}
