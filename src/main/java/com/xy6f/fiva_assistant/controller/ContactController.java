package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.model.Contact;
import com.xy6f.fiva_assistant.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String type,
                                  @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(Map.of("success", true, "data", contactService.search(type, keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", contactService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Contact contact) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", contactService.create(contact)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Contact contact) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", contactService.update(id, contact)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            contactService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
