package Fleet.check.config;

import Fleet.check.entity.*;
import Fleet.check.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChecklistDataSeeder implements CommandLineRunner {

    private final ChecklistTemplateRepository templateRepo;
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedTemplates();
    }

    private void seedRoles() {
        if (roleRepo.count() > 0) return;
        roleRepo.save(new Role(null, "ADMIN"));
        roleRepo.save(new Role(null, "DRIVER"));
        roleRepo.save(new Role(null, "SUPERVISOR"));
        roleRepo.save(new Role(null, "SECURITY"));
    }

    private void seedAdminUser() {
        if (userRepo.existsById("admin")) return;
        Role adminRole = roleRepo.findAll().stream()
                .filter(r -> "ADMIN".equals(r.getName()))
                .findFirst().orElse(null);
        
        User admin = new User();
        admin.setUserId("admin");
        admin.setUsername("admin");
        admin.setFullName("System Admin");
        admin.setRole(adminRole);
        admin.setPinHash(passwordEncoder.encode("1234")); // Default PIN
        userRepo.save(admin);
        log.info("Seeded default admin user (admin/1234)");
    }

    private void seedTemplates() {
        if (templateRepo.count() > 0) {
            log.info("Checklist templates already seeded, skipping.");
            return;
        }
        log.info("Seeding 23 checklist templates...");

        String z1 = "ZONE_1_CAB",   z1n = "Zone 1: CAB";
        String z2 = "ZONE_2_FRONT", z2n = "Zone 2: FRONT";
        String z3 = "ZONE_3_SIDES", z3n = "Zone 3: SIDES";
        String z4 = "ZONE_4_REAR",  z4n = "Zone 4: REAR";

        seed(z1, z1n, "GAUGES",            "Gauges",             "Check for warning lights on dashboard (Engine, ABS, Oil).",                true,  false);
        seed(z1, z1n, "HOOTER",            "Hooter",             "Verify the horn is working.",                                              true,  false);
        seed(z1, z1n, "REVERSE_BUZZER",    "Reverse Buzzer",     "Confirm the audible backing alarm is functional.",                         true,  false);
        seed(z1, z1n, "SEAT_BELTS",        "Seat Belts",         "Ensure all belts are fitted and in working condition.",                    true,  false);
        seed(z1, z1n, "IN_CAB_5S",         "In-Cab 5S",          "Is the cabin clean, neat, and presentable?",                              false, false);

        seed(z2, z2n, "FLUIDS_FUEL",       "Fluids (General)",   "Verify enough fuel is present for the planned trip.",                      false, false);
        seed(z2, z2n, "FLUIDS_LEAKS",      "Fluids - Leaks",     "Check for visible oil, water, or diesel leaks under the engine.",          true,  false);
        seed(z2, z2n, "LIGHTS_LENSES",     "Lights & Lenses",    "All forward lights working; lenses not cracked or broken.",                true,  false);
        seed(z2, z2n, "WINDSCREEN_WIPERS", "Windscreen & Wipers","No cracks on the glass; wipers and washers functional.",                   true,  false);
        seed(z2, z2n, "MIRRORS",           "Mirrors",            "Mirrors are intact and adjusted correctly.",                               true,  false);

        seed(z3, z3n, "TYRES_RIMS",        "Tyres & Rims",       "Check for visible damage; all wheel nuts in place.",                       true,  false);
        seed(z3, z3n, "TREAD_DEPTH",       "Tread Depth",        "Verify tread is more than 3mm.",                                          true,  false);
        seed(z3, z3n, "SAFETY_EQUIPMENT",  "Safety Equipment",   "First Aid Kit, Fire Extinguisher, Emergency Triangles, Traffic Cones.",    true,  false);
        seed(z3, z3n, "OPERATIONAL_TOOLS", "Operational Tools",  "Chock blocks, pallet steps, and trolleys available.",                      false, false);
        seed(z3, z3n, "LEGAL_COMPLIANCE",  "Legal/Compliance",   "All licenses, operator cards, and insurance cards valid.",                 true,  false);
        seed(z3, z3n, "IDENTIFICATION",    "Identification",     "Fleet numbers and registration plates are intact and visible.",            true,  false);

        seed(z4, z4n, "CHEVRON_BUMPER",    "Chevron & Bumper",   "Rear chevron not bent or damaged; under-ride bumper secure.",              true,  false);
        seed(z4, z4n, "ROOF_TARPS",        "Roof & Tarps",       "Inspect for cuts, damages, or leaks in the trailer covering.",             false, false);
        seed(z4, z4n, "TARP_HARDWARE",     "Tarp Hardware",      "Shock cords in place; buckles operational; tarps close securely.",         false, true);
        seed(z4, z4n, "FIVE_S_COMPLIANCE", "5S Compliance",      "Entire truck and trailer unit is clean and professional.",                 false, false);
        seed(z4, z4n, "FLT_HITCHING",      "FLT Hitching",       "Correctly hitched with locking pin in place. (If applicable)",            true,  false);
        seed(z4, z4n, "FLT_FLUIDS",        "FLT Fluids",         "No visible leaks; hydraulic level above minimum. (If applicable)",        true,  false);
        seed(z4, z4n, "FLT_LEGAL",         "FLT Legal",          "Valid license document and load test certificate. (If applicable)",        true,  false);
        seed(z4, z4n, "FLT_MECHANICAL",    "FLT Mechanical",     "Mast raises and lowers smoothly; no visible damage. (If applicable)",     true,  false);

        log.info("Checklist seeding complete: 23 items across 4 zones.");
    }

    private void seed(String zoneCode, String zoneName, String code, String name, String desc,
                      boolean critical, boolean hitch) {
        if (templateRepo.findByItemCode(code).isEmpty()) {
            templateRepo.save(new ChecklistTemplate(null, zoneCode, zoneName, code, name, desc, critical, hitch, true));
        }
    }
}
