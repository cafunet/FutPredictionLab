package com.futprediction.prediction.entity;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.match.entity.Partido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediccion")
public class Prediccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prediccion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_partido", nullable = false)
    private Partido partido;

    @Column(name = "prob_local", nullable = false, precision = 5, scale = 4)
    private BigDecimal probLocal;

    @Column(name = "prob_empate", nullable = false, precision = 5, scale = 4)
    private BigDecimal probEmpate;

    @Column(name = "prob_visitante", nullable = false, precision = 5, scale = 4)
    private BigDecimal probVisitante;

    @Column(name = "explicacion", columnDefinition = "TEXT")
    private String explicacion;

    @Column(name = "nivel_confianza", length = 30)
    private String nivelConfianza;

    @Column(name = "fecha_prediccion", nullable = false)
    private LocalDateTime fechaPrediccion = LocalDateTime.now();

    @Column(name = "goles_local_previsto")
    private Integer golesLocalPrevisto;

    @Column(name = "goles_visitante_previsto")
    private Integer golesVisitantePrevisto;

    @Column(name = "modelo_version", length = 50)
    private String modeloVersion;

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Partido getPartido() {
        return partido;
    }

    public void setPartido(Partido partido) {
        this.partido = partido;
    }

    public BigDecimal getProbLocal() {
        return probLocal;
    }

    public void setProbLocal(BigDecimal probLocal) {
        this.probLocal = probLocal;
    }

    public BigDecimal getProbEmpate() {
        return probEmpate;
    }

    public void setProbEmpate(BigDecimal probEmpate) {
        this.probEmpate = probEmpate;
    }

    public BigDecimal getProbVisitante() {
        return probVisitante;
    }

    public void setProbVisitante(BigDecimal probVisitante) {
        this.probVisitante = probVisitante;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public void setExplicacion(String explicacion) {
        this.explicacion = explicacion;
    }

    public String getNivelConfianza() {
        return nivelConfianza;
    }

    public void setNivelConfianza(String nivelConfianza) {
        this.nivelConfianza = nivelConfianza;
    }

    public LocalDateTime getFechaPrediccion() {
        return fechaPrediccion;
    }

    public void setFechaPrediccion(LocalDateTime fechaPrediccion) {
        this.fechaPrediccion = fechaPrediccion;
    }

    public Integer getGolesLocalPrevisto() {
        return golesLocalPrevisto;
    }

    public void setGolesLocalPrevisto(Integer golesLocalPrevisto) {
        this.golesLocalPrevisto = golesLocalPrevisto;
    }

    public Integer getGolesVisitantePrevisto() {
        return golesVisitantePrevisto;
    }

    public void setGolesVisitantePrevisto(Integer golesVisitantePrevisto) {
        this.golesVisitantePrevisto = golesVisitantePrevisto;
    }

    public String getModeloVersion() {
        return modeloVersion;
    }

    public void setModeloVersion(String modeloVersion) {
        this.modeloVersion = modeloVersion;
    }
}
