package com.vanokal0.warehouse_backend.mappers;


import com.vanokal0.warehouse_backend.dto.EmployeeDTO;
import com.vanokal0.warehouse_backend.model.Employee;
import com.vanokal0.warehouse_backend.repositories.EmployeePositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper implements ModelDTOMapper<Employee, EmployeeDTO> {

    private final EmployeePositionRepository employeePositionRepository;


    @Autowired
    public EmployeeMapper(EmployeePositionRepository employeePositionRepository){
        this.employeePositionRepository = employeePositionRepository;
    }

    @Override
    public EmployeeDTO toDTO(Employee employee) {
        return new EmployeeDTO(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getPosition().getPositionName(),
                employee.getWarehouse().getNumber()
        );
    }
}
