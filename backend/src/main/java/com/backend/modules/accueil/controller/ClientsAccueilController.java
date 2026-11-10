package com.backend.modules.accueil.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.accueil.dto.ClientsDtos.ClientAccueilDto;
import com.backend.modules.accueil.dto.ClientsDtos.CreerClientRequest;
import com.backend.modules.accueil.dto.ClientsDtos.PatientDto;
import com.backend.modules.accueil.dto.ClientsDtos.PatientRequest;
import com.backend.modules.accueil.dto.ClientsDtos.RattacherClientRequest;
import com.backend.modules.accueil.service.ClientsAccueilService;
import com.backend.modules.accueil.service.PatientsAccueilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Enregistrement des clients et des patients à l'accueil. */
@RestController
@RequestMapping("/api/accueil")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ACCUEIL')")
@Tag(name = "Accueil - clients et patients", description = "Enregistrement des clients et des patients")
public class ClientsAccueilController {

    private final ClientsAccueilService clientsService;
    private final PatientsAccueilService patientsService;

    @GetMapping("/clients")
    @Operation(summary = "Clients du laboratoire")
    public ResponseEntity<ApiResponse<List<ClientAccueilDto>>> clients() {
        return ResponseEntity.ok(ApiResponse.success(clientsService.lister()));
    }

    @PostMapping("/clients")
    @Operation(summary = "Créer un compte client (invitation par e-mail pour choisir son mot de passe)")
    public ResponseEntity<ApiResponse<ClientAccueilDto>> creer(@Valid @RequestBody CreerClientRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Client enregistré", clientsService.creer(request)));
    }

    @PostMapping("/clients/rattacher")
    @Operation(summary = "Rattacher au laboratoire un client déjà inscrit sur la plateforme")
    public ResponseEntity<ApiResponse<ClientAccueilDto>> rattacher(@Valid @RequestBody RattacherClientRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Client rattaché", clientsService.rattacher(request.email())));
    }

    @PostMapping("/clients/{utilisateurId}/invitation")
    @Operation(summary = "Renvoyer le lien d'activation à un client")
    public ResponseEntity<ApiResponse<ClientAccueilDto>> renvoyer(@PathVariable Long utilisateurId) {
        return ResponseEntity.ok(ApiResponse.success("Invitation renvoyée", clientsService.renvoyerInvitation(utilisateurId)));
    }

    @GetMapping("/patients")
    @Operation(summary = "Patients du laboratoire")
    public ResponseEntity<ApiResponse<List<PatientDto>>> patients() {
        return ResponseEntity.ok(ApiResponse.success(patientsService.lister()));
    }

    @PostMapping("/patients")
    @Operation(summary = "Enregistrer un patient")
    public ResponseEntity<ApiResponse<PatientDto>> creerPatient(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Patient enregistré", patientsService.creer(request)));
    }

    @PutMapping("/patients/{id}")
    @Operation(summary = "Modifier la fiche d'un patient")
    public ResponseEntity<ApiResponse<PatientDto>> modifierPatient(@PathVariable Long id,
                                                                   @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Patient modifié", patientsService.modifier(id, request)));
    }
}
