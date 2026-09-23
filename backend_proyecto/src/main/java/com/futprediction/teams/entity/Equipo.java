package com.futprediction.teams.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String pais;

    @Column(nullable = false, length = 10)
    private String grupo;

    @Column(name = "ranking_fifa", nullable = false)
    private Integer rankingFifa;

    @Column(name = "bandera_url", columnDefinition = "TEXT")
    private String banderaUrl;

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public Integer getRankingFifa() {
        return rankingFifa;
    }

    public void setRankingFifa(Integer rankingFifa) {
        this.rankingFifa = rankingFifa;
    }

    public String getBanderaUrl() {
        return banderaUrl;
    }

    public void setBanderaUrl(String banderaUrl) {
        this.banderaUrl = banderaUrl;
    }
}
