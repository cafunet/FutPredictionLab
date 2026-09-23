package com.futprediction.standings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "clasificacion_manual")
public class ClasificacionManual {

    @Id
    @Column(name = "id_equipo")
    private Long idEquipo;

    @Column(nullable = false)
    private boolean activo = false;

    @Column(nullable = false)
    private int pts;

    @Column(nullable = false)
    private int pj;

    @Column(nullable = false)
    private int pg;

    @Column(nullable = false)
    private int pe;

    @Column(nullable = false)
    private int pp;

    @Column(nullable = false)
    private int gf;

    @Column(nullable = false)
    private int gc;

    @Column(name = "id_usuario_ultimo")
    private Long idUsuarioUltimo;

    @Column(name = "fecha_ultimo", nullable = false)
    private LocalDateTime fechaUltimo = LocalDateTime.now();

    @Column(name = "modificado_por", length = 120)
    private String modificadoPor;

    @Column(name = "sello_firma", length = 500)
    private String selloFirma;

    public Long getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Long idEquipo) {
        this.idEquipo = idEquipo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getPts() {
        return pts;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }

    public int getPj() {
        return pj;
    }

    public void setPj(int pj) {
        this.pj = pj;
    }

    public int getPg() {
        return pg;
    }

    public void setPg(int pg) {
        this.pg = pg;
    }

    public int getPe() {
        return pe;
    }

    public void setPe(int pe) {
        this.pe = pe;
    }

    public int getPp() {
        return pp;
    }

    public void setPp(int pp) {
        this.pp = pp;
    }

    public int getGf() {
        return gf;
    }

    public void setGf(int gf) {
        this.gf = gf;
    }

    public int getGc() {
        return gc;
    }

    public void setGc(int gc) {
        this.gc = gc;
    }

    public Long getIdUsuarioUltimo() {
        return idUsuarioUltimo;
    }

    public void setIdUsuarioUltimo(Long idUsuarioUltimo) {
        this.idUsuarioUltimo = idUsuarioUltimo;
    }

    public LocalDateTime getFechaUltimo() {
        return fechaUltimo;
    }

    public void setFechaUltimo(LocalDateTime fechaUltimo) {
        this.fechaUltimo = fechaUltimo;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    public String getSelloFirma() {
        return selloFirma;
    }

    public void setSelloFirma(String selloFirma) {
        this.selloFirma = selloFirma;
    }
}
