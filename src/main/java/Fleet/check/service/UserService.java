package Fleet.check.service;

import Fleet.check.dto.UserDTO;
import Fleet.check.entity.User;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.RoleRepository;
import Fleet.check.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDTO> getAll() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTO> getById(String id) {
        return userRepository.findById(id).map(this::toDTO);
    }

    public UserDTO create(UserDTO dto) {
        User user = new User();
        user.setUserId(dto.getUserId());
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setFleetGroup(dto.getFleetGroup());
        
        if (dto.getRoleId() != null) {
            user.setRole(roleRepository.findById(dto.getRoleId()).orElse(null));
        }
        
        if (dto.getPin() != null) {
            user.setPinHash(passwordEncoder.encode(dto.getPin()));
        }
        
        return toDTO(userRepository.save(user));
    }

    public UserDTO update(String id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());
        user.setFleetGroup(dto.getFleetGroup());
        
        if (dto.getRoleId() != null) {
            user.setRole(roleRepository.findById(dto.getRoleId()).orElse(null));
        }
        
        if (dto.getPin() != null) {
            user.setPinHash(passwordEncoder.encode(dto.getPin()));
        }
        
        return toDTO(userRepository.save(user));
    }

    public void delete(String id) {
        userRepository.deleteById(id);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setFleetGroup(user.getFleetGroup());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getId());
            dto.setRoleName(user.getRole().getName());
        }
        return dto;
    }
}
