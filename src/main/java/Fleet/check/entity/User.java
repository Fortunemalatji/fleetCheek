package Fleet.check.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Id
    @Column(length = 20)
    private String userId; // e.g., 0000827008

    @Column(unique = true, length = 50)
    private String username;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(nullable = false, length = 100)
    private String fullName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fleet_group_id")
    private FleetGroup fleetGroup;

    @Column(length = 255)
    private String pinHash;

    @Column(insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pin; // Only used for incoming requests (creation/update)

    public String getPin() { return this.pin; }
    
    @JsonSetter("pin")
    public void setPin(String pin) { this.pin = pin; }
}
