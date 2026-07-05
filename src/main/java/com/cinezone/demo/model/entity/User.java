package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.Gender;
import com.cinezone.demo.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false)
    private String contrasena;

    @Column(length = 20)
    private String celular;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(unique = true, length = 15)
    private String dni;

    @Column(name = "tipo_documento", length = 20)
    @Builder.Default
    private String tipoDocumento = "DNI";

    @Column(length = 20)
    private String genero; // Ej: "MASCULINO", "FEMENINO", "OTRO"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role rol;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuario_sedes",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "sede_id")
    )
    @Builder.Default
    private Set<Cinema> sedes = new java.util.HashSet<>();

    @Builder.Default
    private Integer puntos = 0;

    @Column(name = "es_socio")
    @Builder.Default
    private Boolean esSocio = false;

    @Column(name = "tiene_discapacidad")
    @Builder.Default
    private Boolean tieneDiscapacidad = false;

    @Column(name = "fecha_registro", updatable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Builder.Default
    private Boolean activo = true;

    @Column(name = "session_token")
    private String sessionToken;

    @Column(name = "verification_code_hash")
    private String verificationCodeHash;

    @Column(name = "verification_expiry")
    private LocalDateTime verificationExpiry;

    @Column(name = "verification_attempts")
    @Builder.Default
    private Integer verificationAttempts = 0;

    // =========================================================
    // Relación con la nueva tabla de niveles (Por defecto será el Azul)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")
    private LoyaltyTier tier;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "uso_beneficios_mensual", columnDefinition = "jsonb")
    @Builder.Default
    private java.util.Map<String, Integer> monthlyBenefitUsage = new java.util.HashMap<>();

    @Column(name = "mes_ultimo_beneficio")
    private Integer lastBenefitMonth;

    // Contador de visitas en el año actual (para subir de nivel)
    @Column(name = "yearly_visits")
    @Builder.Default
    private Integer yearlyVisits = 0;

    // Contador de dinero gastado en dulcería en el año actual
    @Column(name = "consumo_anual_dulceria", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal yearlySnackConsumption = BigDecimal.ZERO;

    // Fecha en la que empezó su año de fidelidad (para saber cuándo resetear contadores)
    @Column(name = "fecha_inicio_periodo")
    @Builder.Default
    private LocalDate periodStartDate = LocalDate.now();

    // Fecha de la última visita (para asegurar 1 visita por día máximo)
    @Column(name = "fecha_ultima_visita")
    private LocalDate lastVisitDate;

    // =========================================================
    // MÉTODOS DE USERDETAILS (SPRING SECURITY)
    // =========================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return activo; }

    public UUID getId() {
        return this.id;
    }


    public void setId(UUID id) {
        this.id = id;
    }


    public String getNombre() {
        return this.nombre;
    }


    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    public String getApellido() {
        return this.apellido;
    }


    public void setApellido(String apellido) {
        this.apellido = apellido;
    }


    public String getCorreo() {
        return this.correo;
    }


    public void setCorreo(String correo) {
        this.correo = correo;
    }


    public String getContrasena() {
        return this.contrasena;
    }


    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }


    public String getCelular() {
        return this.celular;
    }


    public void setCelular(String celular) {
        this.celular = celular;
    }


    public LocalDate getFechaNacimiento() {
        return this.fechaNacimiento;
    }


    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }


    public String getDni() {
        return this.dni;
    }


    public void setDni(String dni) {
        this.dni = dni;
    }


    public String getGenero() {
        return this.genero;
    }


    public void setGenero(String genero) {
        this.genero = genero;
    }


    public Role getRol() {
        return this.rol;
    }


    public void setRol(Role rol) {
        this.rol = rol;
    }





    public LoyaltyTier getTier() {
        return this.tier;
    }


    public void setTier(LoyaltyTier tier) {
        this.tier = tier;
    }


    public Integer getLastBenefitMonth() {
        return this.lastBenefitMonth;
    }


    public void setLastBenefitMonth(Integer lastBenefitMonth) {
        this.lastBenefitMonth = lastBenefitMonth;
    }


    public LocalDate getLastVisitDate() {
        return this.lastVisitDate;
    }


    public void setLastVisitDate(LocalDate lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }

    public Integer getPuntos() { return this.puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
    
    public Boolean getEsSocio() { return this.esSocio; }
    public void setEsSocio(Boolean esSocio) { this.esSocio = esSocio; }
    
    public Integer getYearlyVisits() { return this.yearlyVisits; }
    public void setYearlyVisits(Integer yearlyVisits) { this.yearlyVisits = yearlyVisits; }
    
    public BigDecimal getYearlySnackConsumption() { return this.yearlySnackConsumption; }
    public void setYearlySnackConsumption(BigDecimal yearlySnackConsumption) { this.yearlySnackConsumption = yearlySnackConsumption; }
    
    public java.util.Map<String, Integer> getMonthlyBenefitUsage() { return this.monthlyBenefitUsage; }
    public void setMonthlyBenefitUsage(java.util.Map<String, Integer> monthlyBenefitUsage) { this.monthlyBenefitUsage = monthlyBenefitUsage; }

    public Set<Cinema> getSedes() { return this.sedes; }
    public void setSedes(Set<Cinema> sedes) { this.sedes = sedes; }

    public String getSessionToken() { return this.sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getVerificationCodeHash() { return this.verificationCodeHash; }
    public void setVerificationCodeHash(String verificationCodeHash) { this.verificationCodeHash = verificationCodeHash; }

    public LocalDateTime getVerificationExpiry() { return this.verificationExpiry; }
    public void setVerificationExpiry(LocalDateTime verificationExpiry) { this.verificationExpiry = verificationExpiry; }

    public Integer getVerificationAttempts() { return this.verificationAttempts; }
    public void setVerificationAttempts(Integer verificationAttempts) { this.verificationAttempts = verificationAttempts; }
}