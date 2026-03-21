$root = "src/main/java/Fleet/check"

function Ensure-Dir([string]$path) {
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}

function Write-JavaFile([string]$path, [string]$content) {
    $dir = Split-Path $path -Parent
    Ensure-Dir $dir
    Set-Content -Path $path -Value $content -Encoding UTF8
}

function Pascal([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $value
    }
    return $value.Substring(0, 1).ToUpper() + $value.Substring(1)
}

function To-KebabCase([string]$name) {
    return (($name -creplace '([a-z0-9])([A-Z])', '$1-$2') -replace '([A-Z])([A-Z][a-z])', '$1-$2').ToLower()
}

function Build-EntityField([hashtable]$field) {
    switch ($field.Kind) {
        "manyToOne" {
            return @"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "$($field.Name)_id")
    private $($field.Type) $($field.Name);
"@
        }
        "lob" {
            return @"
    @Lob
    private $($field.Type) $($field.Name);
"@
        }
        default {
            return "    private $($field.Type) $($field.Name);"
        }
    }
}

function Build-DtoField([hashtable]$field) {
    if ($field.Kind -eq "manyToOne") {
        return "    private Long $($field.Name)Id;"
    }
    return "    private $($field.Type) $($field.Name);"
}

function Build-ToDtoLines([hashtable[]]$fields) {
    $lines = @("        dto.setId(entity.getId());")
    foreach ($field in $fields) {
        $setter = "set$(Pascal $field.Name)"
        if ($field.Kind -eq "manyToOne") {
            $dtoSetter = "set$(Pascal $field.Name)Id"
            $lines += "        dto.$dtoSetter(entity.get$((Pascal $field.Name))() != null ? entity.get$((Pascal $field.Name))().getId() : null);"
        } else {
            $lines += "        dto.$setter(entity.get$((Pascal $field.Name))());"
        }
    }
    return $lines -join "`r`n"
}

function Build-UpdateEntityLines([hashtable[]]$fields) {
    $lines = @()
    foreach ($field in $fields) {
        $pascal = Pascal $field.Name
        if ($field.Kind -eq "manyToOne") {
            $lines += "        entity.set$pascal(reference($($field.Type).class, dto.get$pascal`Id()));"
        } else {
            $lines += "        entity.set$pascal(dto.get$pascal());"
        }
    }
    return $lines -join "`r`n"
}

function Build-Entity([hashtable]$meta) {
    $fieldBlock = ($meta.Fields | ForEach-Object { Build-EntityField $_ }) -join "`r`n`r`n"
    return @"
package Fleet.check.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "$($meta.Table)")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class $($meta.Name) extends BaseEntity {

$fieldBlock
}
"@
}

function Build-Dto([hashtable]$meta) {
    $fieldBlock = ($meta.Fields | ForEach-Object { Build-DtoField $_ }) -join "`r`n"
    return @"
package Fleet.check.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class $($meta.Name)Dto {

    private Long id;
$fieldBlock
}
"@
}

function Build-Repository([hashtable]$meta) {
    return @"
package Fleet.check.repository;

import Fleet.check.entity.$($meta.Name);
import org.springframework.data.jpa.repository.JpaRepository;

public interface $($meta.Name)Repository extends JpaRepository<$($meta.Name), Long> {
}
"@
}

function Build-Service([hashtable]$meta) {
    return @"
package Fleet.check.service;

import Fleet.check.dto.$($meta.Name)Dto;

public interface $($meta.Name)Service extends CrudService<$($meta.Name)Dto, Long> {
}
"@
}

function Build-ServiceImpl([hashtable]$meta) {
    $toDto = Build-ToDtoLines $meta.Fields
    $updateEntity = Build-UpdateEntityLines $meta.Fields
    return @"
package Fleet.check.service.impl;

import Fleet.check.dto.$($meta.Name)Dto;
import Fleet.check.entity.*;
import Fleet.check.repository.$($meta.Name)Repository;
import Fleet.check.service.$($meta.Name)Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class $($meta.Name)ServiceImpl extends AbstractCrudService<$($meta.Name), $($meta.Name)Dto> implements $($meta.Name)Service {

    private final $($meta.Name)Repository repository;

    public $($meta.Name)ServiceImpl($($meta.Name)Repository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<$($meta.Name), Long> getRepository() {
        return repository;
    }

    @Override
    protected Long getDtoId($($meta.Name)Dto dto) {
        return dto.getId();
    }

    @Override
    protected $($meta.Name) createEntity() {
        return new $($meta.Name)();
    }

    @Override
    protected $($meta.Name)Dto toDto($($meta.Name) entity) {
        $($meta.Name)Dto dto = new $($meta.Name)Dto();
$toDto
        return dto;
    }

    @Override
    protected void updateEntity($($meta.Name) entity, $($meta.Name)Dto dto) {
$updateEntity
    }
}
"@
}

function Build-Controller([hashtable]$meta) {
    $route = "/api/$(To-KebabCase $meta.Name)s"
    return @"
package Fleet.check.controller;

import Fleet.check.dto.$($meta.Name)Dto;
import Fleet.check.service.$($meta.Name)Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("$route")
public class $($meta.Name)Controller extends AbstractCrudController<$($meta.Name)Dto> {

    private final $($meta.Name)Service service;

    public $($meta.Name)Controller($($meta.Name)Service service) {
        this.service = service;
    }

    @Override
    protected $($meta.Name)Service getService() {
        return service;
    }
}
"@
}

$baseEntity = @"
package Fleet.check.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
"@

$crudService = @"
package Fleet.check.service;

import java.util.List;
import java.util.Optional;

public interface CrudService<D, ID> {

    List<D> findAll();

    Optional<D> findById(ID id);

    D save(D dto);

    void deleteById(ID id);
}
"@

$abstractCrudService = @"
package Fleet.check.service.impl;

import Fleet.check.entity.BaseEntity;
import Fleet.check.service.CrudService;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractCrudService<E extends BaseEntity, D> implements CrudService<D, Long> {

    protected abstract JpaRepository<E, Long> getRepository();

    protected abstract Long getDtoId(D dto);

    protected abstract E createEntity();

    protected abstract D toDto(E entity);

    protected abstract void updateEntity(E entity, D dto);

    @Override
    public List<D> findAll() {
        return getRepository().findAll().stream().map(this::toDto).toList();
    }

    @Override
    public Optional<D> findById(Long id) {
        return getRepository().findById(id).map(this::toDto);
    }

    @Override
    public D save(D dto) {
        E entity = Optional.ofNullable(getDtoId(dto))
            .flatMap(id -> getRepository().findById(id))
            .orElseGet(this::createEntity);
        updateEntity(entity, dto);
        return toDto(getRepository().save(entity));
    }

    @Override
    public void deleteById(Long id) {
        getRepository().deleteById(id);
    }

    protected <T extends BaseEntity> T reference(Class<T> type, Long id) {
        if (id == null) {
            return null;
        }

        try {
            T entity = type.getDeclaredConstructor().newInstance();
            entity.setId(id);
            return entity;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to create reference for " + type.getSimpleName(), exception);
        }
    }
}
"@

$abstractCrudController = @"
package Fleet.check.controller;

import Fleet.check.service.CrudService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class AbstractCrudController<D> {

    protected abstract CrudService<D, Long> getService();

    @GetMapping
    public List<D> getAll() {
        return getService().findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<D> getById(@PathVariable Long id) {
        Optional<D> dto = getService().findById(id);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<D> create(@RequestBody D dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(getService().save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<D> update(@PathVariable Long id, @RequestBody D dto) {
        assignId(dto, id);
        return ResponseEntity.ok(getService().save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        getService().deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void assignId(D dto, Long id) {
        try {
            dto.getClass().getMethod("setId", Long.class).invoke(dto, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("DTO must expose setId(Long)", exception);
        }
    }
}
"@

$entities = @(
    @{ Name = "PersonProfile"; Table = "person_profiles"; Fields = @(
        @{ Name = "firstName"; Type = "String"; Kind = "simple" },
        @{ Name = "lastName"; Type = "String"; Kind = "simple" },
        @{ Name = "email"; Type = "String"; Kind = "simple" },
        @{ Name = "phoneNumber"; Type = "String"; Kind = "simple" },
        @{ Name = "dateOfBirth"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" },
        @{ Name = "organization"; Type = "Organization"; Kind = "manyToOne" },
        @{ Name = "workspace"; Type = "Workspace"; Kind = "manyToOne" }
    )},
    @{ Name = "PersonIdentification"; Table = "person_identifications"; Fields = @(
        @{ Name = "personProfile"; Type = "PersonProfile"; Kind = "manyToOne" },
        @{ Name = "idType"; Type = "String"; Kind = "simple" },
        @{ Name = "idNumber"; Type = "String"; Kind = "simple" },
        @{ Name = "issuingCountry"; Type = "String"; Kind = "simple" },
        @{ Name = "expiryDate"; Type = "LocalDate"; Kind = "simple" }
    )},
    @{ Name = "PasswordResetToken"; Table = "password_reset_tokens"; Fields = @(
        @{ Name = "personProfile"; Type = "PersonProfile"; Kind = "manyToOne" },
        @{ Name = "token"; Type = "String"; Kind = "simple" },
        @{ Name = "expiresAt"; Type = "LocalDateTime"; Kind = "simple" },
        @{ Name = "usedAt"; Type = "LocalDateTime"; Kind = "simple" }
    )},
    @{ Name = "AuditLog"; Table = "audit_logs"; Fields = @(
        @{ Name = "entityName"; Type = "String"; Kind = "simple" },
        @{ Name = "entityIdValue"; Type = "Long"; Kind = "simple" },
        @{ Name = "actionType"; Type = "String"; Kind = "simple" },
        @{ Name = "actorProfile"; Type = "PersonProfile"; Kind = "manyToOne" },
        @{ Name = "actionAt"; Type = "LocalDateTime"; Kind = "simple" },
        @{ Name = "payload"; Type = "String"; Kind = "lob" }
    )},
    @{ Name = "Organization"; Table = "organizations"; Fields = @(
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" }
    )},
    @{ Name = "Workspace"; Table = "workspaces"; Fields = @(
        @{ Name = "organization"; Type = "Organization"; Kind = "manyToOne" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" }
    )},
    @{ Name = "BusinessUnit"; Table = "business_units"; Fields = @(
        @{ Name = "organization"; Type = "Organization"; Kind = "manyToOne" },
        @{ Name = "workspace"; Type = "Workspace"; Kind = "manyToOne" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "manager"; Type = "Employee"; Kind = "manyToOne" }
    )},
    @{ Name = "Employee"; Table = "employees"; Fields = @(
        @{ Name = "personProfile"; Type = "PersonProfile"; Kind = "manyToOne" },
        @{ Name = "employeeNumber"; Type = "String"; Kind = "simple" },
        @{ Name = "organization"; Type = "Organization"; Kind = "manyToOne" },
        @{ Name = "workspace"; Type = "Workspace"; Kind = "manyToOne" },
        @{ Name = "businessUnit"; Type = "BusinessUnit"; Kind = "manyToOne" },
        @{ Name = "manager"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "hireDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "jobTitle"; Type = "String"; Kind = "simple" },
        @{ Name = "employmentStatus"; Type = "StatusType"; Kind = "manyToOne" }
    )},
    @{ Name = "LeaveRequest"; Table = "leave_requests"; Fields = @(
        @{ Name = "employee"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "startDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "endDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "leaveType"; Type = "String"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" },
        @{ Name = "reason"; Type = "String"; Kind = "simple" },
        @{ Name = "approvedBy"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "approvedAt"; Type = "LocalDateTime"; Kind = "simple" }
    )},
    @{ Name = "LeaveBalance"; Table = "leave_balances"; Fields = @(
        @{ Name = "employee"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "leaveType"; Type = "String"; Kind = "simple" },
        @{ Name = "balance"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "accrued"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "used"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "effectiveDate"; Type = "LocalDate"; Kind = "simple" }
    )},
    @{ Name = "PersonQualification"; Table = "person_qualifications"; Fields = @(
        @{ Name = "personProfile"; Type = "PersonProfile"; Kind = "manyToOne" },
        @{ Name = "title"; Type = "String"; Kind = "simple" },
        @{ Name = "institution"; Type = "String"; Kind = "simple" },
        @{ Name = "qualificationType"; Type = "String"; Kind = "simple" },
        @{ Name = "issuedDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "expiryDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "document"; Type = "Document"; Kind = "manyToOne" }
    )},
    @{ Name = "KPIDefinition"; Table = "kpi_definitions"; Fields = @(
        @{ Name = "organization"; Type = "Organization"; Kind = "manyToOne" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" },
        @{ Name = "kpiUnit"; Type = "KPIUnit"; Kind = "manyToOne" },
        @{ Name = "defaultTarget"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "active"; Type = "Boolean"; Kind = "simple" }
    )},
    @{ Name = "PerformanceEvaluation"; Table = "performance_evaluations"; Fields = @(
        @{ Name = "employee"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "evaluator"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "periodStart"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "periodEnd"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" },
        @{ Name = "overallScore"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "comments"; Type = "String"; Kind = "simple" }
    )},
    @{ Name = "PerformanceKPIScore"; Table = "performance_kpi_scores"; Fields = @(
        @{ Name = "performanceEvaluation"; Type = "PerformanceEvaluation"; Kind = "manyToOne" },
        @{ Name = "kpiDefinition"; Type = "KPIDefinition"; Kind = "manyToOne" },
        @{ Name = "targetValue"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "actualValue"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "score"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "comments"; Type = "String"; Kind = "simple" }
    )},
    @{ Name = "EvaluationSkill"; Table = "evaluation_skills"; Fields = @(
        @{ Name = "performanceEvaluation"; Type = "PerformanceEvaluation"; Kind = "manyToOne" },
        @{ Name = "skillName"; Type = "String"; Kind = "simple" },
        @{ Name = "rating"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "comments"; Type = "String"; Kind = "simple" }
    )},
    @{ Name = "Project"; Table = "projects"; Fields = @(
        @{ Name = "organization"; Type = "Organization"; Kind = "manyToOne" },
        @{ Name = "workspace"; Type = "Workspace"; Kind = "manyToOne" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" },
        @{ Name = "startDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "endDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" },
        @{ Name = "billable"; Type = "Boolean"; Kind = "simple" }
    )},
    @{ Name = "ProjectTask"; Table = "project_tasks"; Fields = @(
        @{ Name = "project"; Type = "Project"; Kind = "manyToOne" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" },
        @{ Name = "startDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "dueDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" },
        @{ Name = "assignee"; Type = "Employee"; Kind = "manyToOne" }
    )},
    @{ Name = "ProjectMember"; Table = "project_members"; Fields = @(
        @{ Name = "project"; Type = "Project"; Kind = "manyToOne" },
        @{ Name = "employee"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "roleType"; Type = "RoleType"; Kind = "manyToOne" },
        @{ Name = "allocationPercentage"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "startDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "endDate"; Type = "LocalDate"; Kind = "simple" }
    )},
    @{ Name = "TimeEntry"; Table = "time_entries"; Fields = @(
        @{ Name = "projectTask"; Type = "ProjectTask"; Kind = "manyToOne" },
        @{ Name = "employee"; Type = "Employee"; Kind = "manyToOne" },
        @{ Name = "entryDate"; Type = "LocalDate"; Kind = "simple" },
        @{ Name = "hoursWorked"; Type = "BigDecimal"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" },
        @{ Name = "statusType"; Type = "StatusType"; Kind = "manyToOne" }
    )},
    @{ Name = "StatusType"; Table = "status_types"; Fields = @(
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" }
    )},
    @{ Name = "RoleType"; Table = "role_types"; Fields = @(
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" }
    )},
    @{ Name = "Document"; Table = "documents"; Fields = @(
        @{ Name = "fileName"; Type = "String"; Kind = "simple" },
        @{ Name = "storageKey"; Type = "String"; Kind = "simple" },
        @{ Name = "contentType"; Type = "String"; Kind = "simple" },
        @{ Name = "fileSize"; Type = "Long"; Kind = "simple" },
        @{ Name = "uploadedBy"; Type = "PersonProfile"; Kind = "manyToOne" },
        @{ Name = "uploadedAt"; Type = "LocalDateTime"; Kind = "simple" }
    )},
    @{ Name = "KPIUnit"; Table = "kpi_units"; Fields = @(
        @{ Name = "code"; Type = "String"; Kind = "simple" },
        @{ Name = "name"; Type = "String"; Kind = "simple" },
        @{ Name = "description"; Type = "String"; Kind = "simple" }
    )}
)

Write-JavaFile "$root/entity/BaseEntity.java" $baseEntity
Write-JavaFile "$root/service/CrudService.java" $crudService
Write-JavaFile "$root/service/impl/AbstractCrudService.java" $abstractCrudService
Write-JavaFile "$root/controller/AbstractCrudController.java" $abstractCrudController

foreach ($entity in $entities) {
    Write-JavaFile "$root/entity/$($entity.Name).java" (Build-Entity $entity)
    Write-JavaFile "$root/dto/$($entity.Name)Dto.java" (Build-Dto $entity)
    Write-JavaFile "$root/repository/$($entity.Name)Repository.java" (Build-Repository $entity)
    Write-JavaFile "$root/service/$($entity.Name)Service.java" (Build-Service $entity)
    Write-JavaFile "$root/service/impl/$($entity.Name)ServiceImpl.java" (Build-ServiceImpl $entity)
    Write-JavaFile "$root/controller/$($entity.Name)Controller.java" (Build-Controller $entity)
}
