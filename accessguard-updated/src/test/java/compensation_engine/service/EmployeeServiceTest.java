package compensation_engine.service;

import compensation_engine.exception.AmbiguousEmployeeNameException;
import compensation_engine.exception.EmployeeNotFoundException;
import compensation_engine.model.Employee;
import compensation_engine.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository);
    }

    @Test
    @DisplayName("resolveEmployeeId with employeeId should verify existence and return ID")
    void testResolveById() {
        Employee emp = new Employee("emp-101", "John Doe", "Eng", "Dev", "ACTIVE");
        when(employeeRepository.findById("emp-101")).thenReturn(Optional.of(emp));

        String resolved = employeeService.resolveEmployeeId("emp-101", "");
        assertEquals("emp-101", resolved);
    }

    @Test
    @DisplayName("resolveEmployeeId with unique name should return matched employee ID")
    void testResolveByUniqueName() {
        Employee emp = new Employee("custom-id-999", "Alice Smith", "Security", "Analyst", "ACTIVE");
        when(employeeRepository.findByNameIgnoreCase("Alice Smith")).thenReturn(List.of(emp));

        String resolved = employeeService.resolveEmployeeId(null, "Alice Smith");
        assertEquals("custom-id-999", resolved);
    }

    @Test
    @DisplayName("resolveEmployeeId with ambiguous name should throw AmbiguousEmployeeNameException with matching IDs")
    void testResolveByAmbiguousName() {
        Employee emp1 = new Employee("emp-01", "John Doe", "Sales", "Rep", "ACTIVE");
        Employee emp2 = new Employee("emp-02", "John Doe", "Eng", "Dev", "ACTIVE");
        when(employeeRepository.findByNameIgnoreCase("John Doe")).thenReturn(List.of(emp1, emp2));

        AmbiguousEmployeeNameException ex = assertThrows(
                AmbiguousEmployeeNameException.class,
                () -> employeeService.resolveEmployeeId("", "John Doe")
        );

        assertTrue(ex.getMatchingIds().contains("emp-01"));
        assertTrue(ex.getMatchingIds().contains("emp-02"));
    }

    @Test
    @DisplayName("resolveEmployeeId with non-existent name should throw EmployeeNotFoundException")
    void testResolveNotFound() {
        when(employeeRepository.findByNameIgnoreCase("Ghost")).thenReturn(List.of());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.resolveEmployeeId(null, "Ghost")
        );
    }
}
