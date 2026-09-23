package com.futprediction.audit.service;

import com.futprediction.audit.dto.AuditLogDTO;
import com.futprediction.audit.entity.Auditoria;
import com.futprediction.audit.repository.AuditoriaRepository;
import com.futprediction.auth.entity.Usuario;
import com.futprediction.match.support.MatchColombiaTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public void log(
            Usuario actor,
            String action,
            String entity,
            String entityId,
            String detail,
            String auditSignature) {
        Auditoria row = new Auditoria();
        if (actor != null) {
            row.setIdUsuario(actor.getId());
            row.setEmailUsuario(actor.getEmail());
            row.setNombreUsuario(actor.getNombre());
        }
        row.setAccion(action);
        row.setEntidad(entity);
        row.setEntidadId(entityId);
        row.setDetalle(detail);
        row.setFirmaReferencia(auditSignature);
        row.setFecha(MatchColombiaTime.now());
        auditoriaRepository.save(row);
    }

    public List<AuditLogDTO> listRecent() {
        return auditoriaRepository.findTop100ByOrderByFechaDesc().stream().map(this::toDto).toList();
    }

    private AuditLogDTO toDto(Auditoria row) {
        return new AuditLogDTO(
                String.valueOf(row.getId()),
                row.getIdUsuario() != null ? String.valueOf(row.getIdUsuario()) : null,
                row.getEmailUsuario(),
                row.getNombreUsuario(),
                row.getAccion(),
                row.getEntidad(),
                row.getEntidadId(),
                row.getDetalle(),
                row.getFirmaReferencia(),
                row.getFecha().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
