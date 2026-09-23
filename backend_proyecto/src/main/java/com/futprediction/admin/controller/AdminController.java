package com.futprediction.admin.controller;

import com.futprediction.admin.dto.AdminCreateUserRequestDTO;
import com.futprediction.admin.dto.AdminCreateUserResponseDTO;
import com.futprediction.admin.dto.UpdateUserRoleRequestDTO;
import com.futprediction.admin.dto.AdminPredictionViewDTO;
import com.futprediction.admin.service.AdminUserService;
import com.futprediction.prediction.service.PredictionIntegrationService;
import com.futprediction.standings.dto.StandingDTO;
import com.futprediction.standings.dto.UpdateStandingRequestDTO;
import com.futprediction.standings.service.StandingsService;
import com.futprediction.auth.dto.UserResponseDTO;
import com.futprediction.auth.entity.Usuario;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminUserService adminUserService;
    private final PredictionIntegrationService predictionIntegrationService;
    private final StandingsService standingsService;

    public AdminController(
            AdminUserService adminUserService,
            PredictionIntegrationService predictionIntegrationService,
            StandingsService standingsService) {
        this.adminUserService = adminUserService;
        this.predictionIntegrationService = predictionIntegrationService;
        this.standingsService = standingsService;
    }

    @GetMapping("/users")
    public List<UserResponseDTO> listUsers() {
        return adminUserService.listUsers();
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody AdminCreateUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public UserResponseDTO updateUser(
            @PathVariable Long id,
            @Valid @RequestBody com.futprediction.admin.dto.AdminUpdateUserRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return adminUserService.updateUser(id, request, actor);
    }

    @PutMapping("/users/{id}/role")
    public UserResponseDTO updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequestDTO request) {
        return adminUserService.updateRole(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        adminUserService.deleteUser(id, actor.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/predictions")
    public List<AdminPredictionViewDTO> listPredictions() {
        return predictionIntegrationService.listAllForAdmin();
    }

    @GetMapping("/standings/groups/{group}")
    public List<StandingDTO> groupStandings(@PathVariable String group) {
        return standingsService.getStandingsForGroup(group);
    }

    @PutMapping("/standings/{teamId}")
    public StandingDTO updateStanding(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateStandingRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return standingsService.updateManualStanding(teamId, request, actor);
    }

    @DeleteMapping("/standings/{teamId}/manual")
    public StandingDTO resetStanding(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Usuario actor) {
        return standingsService.resetManualStanding(teamId, actor);
    }
}
