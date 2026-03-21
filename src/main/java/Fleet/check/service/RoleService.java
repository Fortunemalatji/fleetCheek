package Fleet.check.service;

import Fleet.check.dto.LookupDTO;
import Fleet.check.entity.Role;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public List<LookupDTO> getAll() {
        return roleRepository.findAll().stream()
                .map(r -> new LookupDTO(r.getId(), r.getName()))
                .collect(Collectors.toList());
    }

    public Role create(Role role) {
        return roleRepository.save(role);
    }

    public Role update(Integer id, Role details) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        role.setName(details.getName());
        return roleRepository.save(role);
    }

    public void delete(Integer id) {
        roleRepository.deleteById(id);
    }
}
