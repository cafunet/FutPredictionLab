package com.futprediction.match.entity;

import com.futprediction.teams.entity.Equipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "partido")
public class Partido {

    public enum EstadoPartido {
        PROGRAMADO,
        EN_CURSO,
        SUSPENDIDO,
        FINALIZADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_equipo_local", nullable = false)
    private Equipo equipoLocal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_equipo_visitante", nullable = false)
    private Equipo equipoVisitante;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 120)
    private String estadio;

    @Column(nullable = false, length = 50)
    private String fase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPartido estado;

    /** Momento real en que arrancó el partido (Iniciar ya o hora programada). */
    @Column(name = "inicio_en_vivo")
    private LocalDateTime inicioEnVivo;

    /** Minuto congelado al suspender el partido. */
    @Column(name = "minuto_suspendido")
    private Integer minutoSuspendido;

    @Column(name = "programado_por", length = 120)
    private String programadoPor;

    /** Minutos de adición anunciados (ej. +5). */
    @Column(name = "tiempo_adicion", nullable = false)
    private Integer tiempoAdicion = 0;

    /** Descanso entre tiempos: reloj congelado. */
    @Column(name = "en_descanso", nullable = false)
    private Boolean enDescanso = false;

    /** Minuto mostrado mientras está en descanso o pausa interna. */
    @Column(name = "minuto_congelado")
    private Integer minutoCongelado;

    /** Arranque del segundo tiempo (reanudar tras descanso). */
    @Column(name = "inicio_segundo_tiempo")
    private LocalDateTime inicioSegundoTiempo;

    public Long getId() {
        return id;
    }

    public Equipo getEquipoLocal() {
        return equipoLocal;
    }

    public void setEquipoLocal(Equipo equipoLocal) {
        this.equipoLocal = equipoLocal;
    }

    public Equipo getEquipoVisitante() {
        return equipoVisitante;
    }

    public void setEquipoVisitante(Equipo equipoVisitante) {
        this.equipoVisitante = equipoVisitante;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getEstadio() {
        return estadio;
    }

    public void setEstadio(String estadio) {
        this.estadio = estadio;
    }

    public String getFase() {
        return fase;
    }

    public void setFase(String fase) {
        this.fase = fase;
    }

    public EstadoPartido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPartido estado) {
        this.estado = estado;
    }

    public LocalDateTime getInicioEnVivo() {
        return inicioEnVivo;
    }

    public void setInicioEnVivo(LocalDateTime inicioEnVivo) {
        this.inicioEnVivo = inicioEnVivo;
    }

    public Integer getMinutoSuspendido() {
        return minutoSuspendido;
    }

    public void setMinutoSuspendido(Integer minutoSuspendido) {
        this.minutoSuspendido = minutoSuspendido;
    }

    public String getProgramadoPor() {
        return programadoPor;
    }

    public void setProgramadoPor(String programadoPor) {
        this.programadoPor = programadoPor;
    }

    public Integer getTiempoAdicion() {
        return tiempoAdicion;
    }

    public void setTiempoAdicion(Integer tiempoAdicion) {
        this.tiempoAdicion = tiempoAdicion;
    }

    public Boolean getEnDescanso() {
        return enDescanso;
    }

    public void setEnDescanso(Boolean enDescanso) {
        this.enDescanso = enDescanso;
    }

    public Integer getMinutoCongelado() {
        return minutoCongelado;
    }

    public void setMinutoCongelado(Integer minutoCongelado) {
        this.minutoCongelado = minutoCongelado;
    }

    public LocalDateTime getInicioSegundoTiempo() {
        return inicioSegundoTiempo;
    }

    public void setInicioSegundoTiempo(LocalDateTime inicioSegundoTiempo) {
        this.inicioSegundoTiempo = inicioSegundoTiempo;
    }
}
