package gov.prajadisha.backend.org.controller;

import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> list() {
        return departmentService.list();
    }

    @PostMapping
    public ResponseEntity<Department> create(@Valid @RequestBody Department department) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(department));
    }

    @PutMapping("/{id}")
    public Department update(@PathVariable String id, @Valid @RequestBody Department department) {
        return departmentService.update(id, department);
    }
}
