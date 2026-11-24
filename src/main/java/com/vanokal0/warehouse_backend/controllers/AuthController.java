package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.model.ApplicationUser;
import com.vanokal0.warehouse_backend.services.ApplicationUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ApplicationUserService userService;

    @Autowired
    public AuthController(ApplicationUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpSession session) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Optional<ApplicationUser> userOpt = userService.authenticate(username, password);

        if (userOpt.isPresent()) {
            ApplicationUser user = userOpt.get();

            return ResponseEntity.ok(Map.of(
                    "fullName", user.getEmployee().getFirstName()+" "+user.getEmployee().getLastName(),
                    "employeeId", user.getEmployee().getId(),
                    "warehouseNumber", user.getEmployee().getWarehouse().getNumber(),
                    "role", user.getEmployee().getPosition().getPositionName()
            ));
        } else {
            return ResponseEntity.status(401).body("Невірний логін або пароль");
        }
    }
}
