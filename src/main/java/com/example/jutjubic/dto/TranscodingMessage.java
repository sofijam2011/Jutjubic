package com.example.jutjubic.dto;

import java.io.Serializable;

public class TranscodingMessage implements Serializable {

    private Long videoId;
    private String originalVideoPath;
    private String outputVideoPath;
    private TranscodingParams params;

    public TranscodingMessage() {}

    public TranscodingMessage(Long videoId, String originalVideoPath, String outputVideoPath, TranscodingParams params) {
        this.videoId = videoId;
        this.originalVideoPath = originalVideoPath;
        this.outputVideoPath = outputVideoPath;
        this.params = params;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getOriginalVideoPath() {
        return originalVideoPath;
    }

    public void setOriginalVideoPath(String originalVideoPath) {
        this.originalVideoPath = originalVideoPath;
    }

    public String getOutputVideoPath() {
        return outputVideoPath;
    }

    public void setOutputVideoPath(String outputVideoPath) {
        this.outputVideoPath = outputVideoPath;
    }

    public TranscodingParams getParams() {
        return params;
    }

    public void setParams(TranscodingParams params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "TranscodingMessage{" +
                "videoId=" + videoId +
                ", originalVideoPath='" + originalVideoPath + '\'' +
                ", outputVideoPath='" + outputVideoPath + '\'' +
                ", params=" + params +
                '}';
    }

    public static class TranscodingParams implements Serializable {
        private String codec;
        private String resolution;
        private String bitrate;
        private String audioCodec;
        private String audioBitrate;
        private String format;

        public TranscodingParams() {}

        public TranscodingParams(String codec, String resolution, String bitrate, String audioCodec, String audioBitrate, String format) {
            this.codec = codec;
            this.resolution = resolution;
            this.bitrate = bitrate;
            this.audioCodec = audioCodec;
            this.audioBitrate = audioBitrate;
            this.format = format;
        }

        public static TranscodingParams default720p() {
            return new TranscodingParams(
                    "libx264",
                    "1280x720",
                    "2000k",
                    "aac",
                    "128k",
                    "mp4"
            );
        }

        public static TranscodingParams default1080p() {
            return new TranscodingParams(
                    "libx264",
                    "1920x1080",
                    "4000k",
                    "aac",
                    "192k",
                    "mp4"
            );
        }

        public String getCodec() {
            return codec;
        }

        public void setCodec(String codec) {
            this.codec = codec;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getBitrate() {
            return bitrate;
        }

        public void setBitrate(String bitrate) {
            this.bitrate = bitrate;
        }

        public String getAudioCodec() {
            return audioCodec;
        }

        public void setAudioCodec(String audioCodec) {
            this.audioCodec = audioCodec;
        }

        public String getAudioBitrate() {
            return audioBitrate;
        }

        public void setAudioBitrate(String audioBitrate) {
            this.audioBitrate = audioBitrate;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        @Override
        public String toString() {
            return "TranscodingParams{" +
                    "codec='" + codec + '\'' +
                    ", resolution='" + resolution + '\'' +
                    ", bitrate='" + bitrate + '\'' +
                    ", audioCodec='" + audioCodec + '\'' +
                    ", audioBitrate='" + audioBitrate + '\'' +
                    ", format='" + format + '\'' +
                    '}';
        }
    }
}
