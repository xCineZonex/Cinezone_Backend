package com.cinezone.demo.api;

import com.cinezone.demo.service.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sedes")
@RequiredArgsConstructor
public class AdminSedeController {

    private final AdminCatalogService adminCatalogService;

    @PatchMapping("/{id}/beneficio-vip-cumpleanos")
    @PreAuthorize("hasRole('ADMIN_SEDE')")
    public ResponseEntity<Void> toggleBeneficioVip(@PathVariable Long id, @RequestParam boolean habilitado) {
        adminCatalogService.toggleBeneficioVipCumpleanos(id, habilitado);
        return ResponseEntity.ok().build();
    }
}
