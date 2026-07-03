package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.StaffRegisterRequestDTO;
import com.cinezone.demo.dto.UserProfileResponseDTO;
import com.cinezone.demo.dto.UserUpdateDTO;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.model.entity.Cinema;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.Role;
import com.cinezone.demo.repository.CinemaRepository;
import com.cinezone.demo.repository.UserRepository;
import com.cinezone.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CinemaRepository cinemaRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentAuthenticatedUser() {
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByCorreo(emailAutenticado)
                .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado"));
    }

    private void validateHierarchyAndScope(User currentUser, User targetUser) {
        if (currentUser.getRol() == Role.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getRol() == Role.ADMIN_SEDE) {
            if (targetUser.getRol() == Role.SUPER_ADMIN) {
                throw new AccessDeniedException("No tienes permiso para interactuar con un SUPER_ADMIN");
            }
            boolean sharesSede = targetUser.getSedes().stream()
                    .anyMatch(sede -> currentUser.getSedes().contains(sede));
            if (!sharesSede && !targetUser.getSedes().isEmpty()) {
                throw new AccessDeniedException("No tienes permiso sobre este usuario, no pertenece a tus sedes");
            }
            return;
        }

        if (currentUser.getRol() == Role.JEFE_SALA) {
            List<Role> allowedRoles = List.of(Role.TAQUILLA, Role.DULCERIA, Role.PORTERO, Role.SOPORTE);
            if (!allowedRoles.contains(targetUser.getRol())) {
                throw new AccessDeniedException("Solo puedes interactuar con roles inferiores en tu sede");
            }
            boolean sharesSede = targetUser.getSedes().stream()
                    .anyMatch(sede -> currentUser.getSedes().contains(sede));
            if (!sharesSede && !targetUser.getSedes().isEmpty()) {
                throw new AccessDeniedException("El usuario no pertenece a tu sede");
            }
            return;
        }

        throw new AccessDeniedException("No tienes permisos suficientes");
    }

    private void validateDocument(String tipoDoc, String numDoc) {
        if (tipoDoc == null) return;
        if ("DNI".equalsIgnoreCase(tipoDoc)) {
            if (!numDoc.matches("\\d{8}")) throw new BusinessRuleException("El DNI debe tener exactamente 8 dígitos.");
        } else if ("PASAPORTE".equalsIgnoreCase(tipoDoc)) {
            if (numDoc.length() < 6 || numDoc.length() > 15) throw new BusinessRuleException("El pasaporte debe tener entre 6 y 15 caracteres.");
        } else if ("CE".equalsIgnoreCase(tipoDoc) || "CARNET DE EXTRANJERIA".equalsIgnoreCase(tipoDoc)) {
            if (numDoc.length() < 6 || numDoc.length() > 15) throw new BusinessRuleException("El Carnet de Extranjería debe tener entre 6 y 15 caracteres.");
        } else {
            throw new BusinessRuleException("Tipo de documento inválido.");
        }
    }

    private boolean canViewUser(User currentUser, User targetUser) {
        if (currentUser.getRol() == Role.SUPER_ADMIN) return true;
        if (currentUser.getRol() == Role.ADMIN_SEDE) {
            if (targetUser.getRol() == Role.SUPER_ADMIN) return false;
            if (targetUser.getSedes().isEmpty()) return true;
            return targetUser.getSedes().stream().anyMatch(s -> currentUser.getSedes().contains(s));
        }
        if (currentUser.getRol() == Role.JEFE_SALA) {
            List<Role> allowedRoles = List.of(Role.TAQUILLA, Role.DULCERIA, Role.PORTERO, Role.SOPORTE);
            if (!allowedRoles.contains(targetUser.getRol())) return false;
            if (targetUser.getSedes().isEmpty()) return true;
            return targetUser.getSedes().stream().anyMatch(s -> currentUser.getSedes().contains(s));
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfile(String email) {
        User user = getUserByEmail(email);
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO registerStaff(StaffRegisterRequestDTO request) {
        User currentUser = getCurrentAuthenticatedUser();

        if (currentUser.getRol() == Role.JEFE_SALA) {
            List<Role> allowedRoles = List.of(Role.TAQUILLA, Role.DULCERIA, Role.PORTERO, Role.SOPORTE);
            if (!allowedRoles.contains(request.rol())) {
                throw new AccessDeniedException("JEFE_SALA solo puede crear roles inferiores");
            }
        } else if (currentUser.getRol() == Role.ADMIN_SEDE) {
            if (request.rol() == Role.SUPER_ADMIN) {
                throw new AccessDeniedException("ADMIN_SEDE no puede crear SUPER_ADMIN");
            }
        } else if (currentUser.getRol() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("No tienes permisos para crear staff");
        }

        if (userRepository.existsByCorreo(request.correo())) {
            throw new BusinessRuleException("El correo ya está registrado.");
        }
        if (userRepository.existsByDni(request.dni())) {
            throw new BusinessRuleException("El DNI ya está registrado.");
        }
        if (request.rol() == Role.CLIENT) {
            throw new BusinessRuleException("Use el registro público para crear clientes.");
        }

        Set<Cinema> sedesToAssign = new HashSet<>();
        if (request.sedesIds() != null && !request.sedesIds().isEmpty()) {
            sedesToAssign = new HashSet<>(cinemaRepository.findAllById(request.sedesIds()));
            if (currentUser.getRol() == Role.ADMIN_SEDE || currentUser.getRol() == Role.JEFE_SALA) {
                boolean ownsAll = currentUser.getSedes().containsAll(sedesToAssign);
                if (!ownsAll) {
                    throw new AccessDeniedException("No puedes asignar una sede que no te pertenece");
                }
                if (sedesToAssign.size() > 1) {
                    throw new BusinessRuleException("Un Administrador de Sede o Jefe de Sala solo puede asignar 1 sede por usuario.");
                }
            }
        } else if (currentUser.getRol() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Debes asignar al menos una de tus sedes al nuevo usuario");
        }

        String tipoDoc = request.tipoDocumento() != null ? request.tipoDocumento() : "DNI";
        validateDocument(tipoDoc, request.dni());

        User staff = new User();
        staff.setNombre(request.nombre());
        staff.setApellido(request.apellido());
        staff.setCorreo(request.correo());
        staff.setTipoDocumento(tipoDoc);
        staff.setDni(request.dni());
        staff.setCelular(request.celular());
        staff.setContrasena(passwordEncoder.encode(request.contrasena()));
        staff.setRol(request.rol());
        staff.setPuntos(0);
        staff.setEsSocio(false);
        staff.setSedes(sedesToAssign);

        User savedStaff = userRepository.save(staff);
        return mapToDTO(savedStaff);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfileByDni(String dni) {
        User user = userRepository.findByDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con DNI: " + dni));
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO createBasicClient(String dni, String nombre) {
        if (userRepository.existsByDni(dni)) {
            throw new BusinessRuleException("El DNI ya está registrado.");
        }
        
        String fakeEmail = dni + "@cliente.cinezone.pe";
        if (userRepository.existsByCorreo(fakeEmail)) {
            fakeEmail = java.util.UUID.randomUUID().toString().substring(0, 8) + "@cliente.cinezone.pe";
        }

        User newClient = new User();
        newClient.setNombre(nombre != null && !nombre.isBlank() ? nombre : "Cliente");
        newClient.setApellido("");
        newClient.setDni(dni);
        newClient.setCorreo(fakeEmail);
        newClient.setContrasena(passwordEncoder.encode(dni));
        newClient.setRol(Role.CLIENT);
        newClient.setEsSocio(false);
        newClient.setPuntos(0);

        User savedClient = userRepository.save(newClient);
        return mapToDTO(savedClient);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO updateProfile(String email, UserUpdateDTO request) {
        User user = getUserByEmail(email);

        user.setNombre(request.nombre());
        user.setApellido(request.apellido());
        user.setCelular(request.celular());
        user.setFechaNacimiento(request.fechaNacimiento());
        user.setGenero(request.genero());

        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO updateMyProfile(UserUpdateDTO request) {
        User user = getCurrentAuthenticatedUser();

        if (request.correo() != null && !request.correo().equals(user.getCorreo())) {
            if (userRepository.existsByCorreo(request.correo())) {
                throw new BusinessRuleException("Error: El nuevo correo ya está en uso por otra persona.");
            }
            user.setCorreo(request.correo());
        }

        if (request.dni() != null && !request.dni().equals(user.getDni())) {
            if (userRepository.existsByDni(request.dni())) {
                throw new BusinessRuleException("Error: El nuevo DNI ya está registrado.");
            }
            String tipoDoc = request.tipoDocumento() != null ? request.tipoDocumento() : user.getTipoDocumento();
            validateDocument(tipoDoc, request.dni());
            user.setTipoDocumento(tipoDoc);
            user.setDni(request.dni());
        } else if (request.tipoDocumento() != null && !request.tipoDocumento().equals(user.getTipoDocumento())) {
            validateDocument(request.tipoDocumento(), user.getDni());
            user.setTipoDocumento(request.tipoDocumento());
        }

        if (request.nombre() != null) user.setNombre(request.nombre());
        if (request.apellido() != null) user.setApellido(request.apellido());
        if (request.celular() != null) user.setCelular(request.celular());
        if (request.fechaNacimiento() != null) user.setFechaNacimiento(request.fechaNacimiento());
        if (request.genero() != null) user.setGenero(request.genero());

        if (request.contrasena() != null && !request.contrasena().isBlank()) {
            user.setContrasena(passwordEncoder.encode(request.contrasena()));
        }

        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByCorreo(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private UserProfileResponseDTO mapToDTO(User user) {
        return new UserProfileResponseDTO(
                user.getId(), user.getNombre(), user.getApellido(),
                user.getCorreo(), user.getDni(), user.getTipoDocumento(), user.getCelular(),
                user.getFechaNacimiento(), user.getGenero(),
                user.getPuntos(),
                user.getTier() != null ? user.getTier().getName() : "Sin Nivel",
                user.getYearlyVisits(), user.getYearlySnackConsumption(),
                user.getRol().name(),
                user.getTier() != null ? 4 : 0,
                user.getMonthlyBenefitUsage(),
                user.getSedes().stream().map(Cinema::getId).collect(Collectors.toList()),
                user.getActivo()
        );
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        User currentUser = getCurrentAuthenticatedUser();
        validateHierarchyAndScope(currentUser, targetUser);
        
        boolean newStatus = !targetUser.getActivo();
        targetUser.setActivo(newStatus);
        
        if (!newStatus) {
            targetUser.setSessionToken(null);
        }
        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public void changeUserPassword(UUID id, String newPassword) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        User currentUser = getCurrentAuthenticatedUser();
        validateHierarchyAndScope(currentUser, targetUser);

        targetUser.setContrasena(passwordEncoder.encode(newPassword));
        userRepository.save(targetUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponseDTO> getAllUsers() {
        User currentUser = getCurrentAuthenticatedUser();
        return userRepository.findAll().stream()
                .filter(user -> user.getRol() != Role.CLIENT)
                .filter(user -> canViewUser(currentUser, user))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        User currentUser = getCurrentAuthenticatedUser();
        validateHierarchyAndScope(currentUser, user);
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO updateUserAsAdmin(UUID id, com.cinezone.demo.dto.UserAdminUpdateDTO request) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        
        User currentUser = getCurrentAuthenticatedUser();
        validateHierarchyAndScope(currentUser, targetUser);

        if (currentUser.getRol() == Role.ADMIN_SEDE && request.rol() == Role.SUPER_ADMIN) {
             throw new AccessDeniedException("ADMIN_SEDE no puede asignar rol SUPER_ADMIN");
        }
        if (currentUser.getRol() == Role.JEFE_SALA) {
             List<Role> allowedRoles = List.of(Role.TAQUILLA, Role.DULCERIA, Role.PORTERO, Role.SOPORTE);
             if (!allowedRoles.contains(request.rol())) {
                 throw new AccessDeniedException("JEFE_SALA solo puede asignar roles inferiores");
             }
        }
        
        if (!targetUser.getCorreo().equals(request.correo()) && userRepository.existsByCorreo(request.correo())) {
            throw new BusinessRuleException("El correo ya está en uso.");
        }
        
        if (targetUser.getDni() != null && !targetUser.getDni().equals(request.dni()) && userRepository.existsByDni(request.dni())) {
            throw new BusinessRuleException("El DNI ya está en uso.");
        }

        if (request.sedesIds() != null) {
             Set<Cinema> sedesToAssign = new HashSet<>(cinemaRepository.findAllById(request.sedesIds()));
             if (currentUser.getRol() == Role.ADMIN_SEDE || currentUser.getRol() == Role.JEFE_SALA) {
                 boolean ownsAll = currentUser.getSedes().containsAll(sedesToAssign);
                 if (!ownsAll) {
                     throw new AccessDeniedException("No puedes asignar una sede que no te pertenece");
                 }
                 if (sedesToAssign.size() > 1) {
                     throw new BusinessRuleException("Un Administrador de Sede o Jefe de Sala solo puede asignar 1 sede por usuario.");
                 }
             }
             targetUser.setSedes(sedesToAssign);
        }

        String tipoDoc = request.tipoDocumento() != null ? request.tipoDocumento() : (targetUser.getTipoDocumento() != null ? targetUser.getTipoDocumento() : "DNI");
        validateDocument(tipoDoc, request.dni());

        targetUser.setNombre(request.nombre());
        targetUser.setApellido(request.apellido());
        targetUser.setCorreo(request.correo());
        targetUser.setTipoDocumento(tipoDoc);
        targetUser.setDni(request.dni());
        targetUser.setCelular(request.celular());
        targetUser.setRol(request.rol());

        targetUser = userRepository.save(targetUser);
        return mapToDTO(targetUser);
    }
}