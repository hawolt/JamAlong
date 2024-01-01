package com.hawolt.source.impl;

import com.hawolt.source.Audio;

public class SimpleAudio implements Audio {
    private final byte[] bytes;
    private final String source, name;

    public SimpleAudio(String source, String name, byte[] bytes) {
        this.source = source;
        this.bytes = bytes;
        this.name = name;
    }

    @Override
    public byte[] data() {
        return bytes;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String source() {
        return source;
    }
}
