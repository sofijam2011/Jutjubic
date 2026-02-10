package com.example.jutjubic.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UploadEvent implements Serializable {
    private Long videoId;
    private String naziv;
    private Long velicina;        // u bajtovima
    private String autor;         // username
    private LocalDateTime timestamp;

    public UploadEvent() {}

    public UploadEvent(Long videoId, String naziv, Long velicina, String autor) {
        this.videoId = videoId;
        this.naziv = naziv;
        this.velicina = velicina;
        this.autor = autor;
        this.timestamp = LocalDateTime.now();
    }

    public UploadEvent(Long videoId, String naziv, Long velicina, String autor, LocalDateTime timestamp) {
        this.videoId = videoId;
        this.naziv = naziv;
        this.velicina = velicina;
        this.autor = autor;
        this.timestamp = timestamp;
    }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getNaziv() { return naziv; }
    public void setNaziv(String naziv) { this.naziv = naziv; }

    public Long getVelicina() { return velicina; }
    public void setVelicina(Long velicina) { this.velicina = velicina; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "UploadEvent{" +
                "videoId=" + videoId +
                ", naziv='" + naziv + '\'' +
                ", velicina=" + velicina +
                ", autor='" + autor + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
