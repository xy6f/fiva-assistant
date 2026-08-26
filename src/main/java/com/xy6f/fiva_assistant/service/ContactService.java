package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Contact;
import com.xy6f.fiva_assistant.mapper.ContactMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactService {

    private final ContactMapper contactMapper;

    public List<Contact> listAll() {
        return contactMapper.findByUserIdOrderByCreatedAtDesc(UserContext.getUserId());
    }

    public List<Contact> listByType(String type) {
        if (type == null || type.isBlank() || "ALL".equals(type)) {
            return listAll();
        }
        return contactMapper.findByUserIdAndTypeOrderByCreatedAtDesc(UserContext.getUserId(), type);
    }

    public List<Contact> search(String type, String keyword) {
        Long userId = UserContext.getUserId();
        if (keyword == null || keyword.isBlank()) {
            return listByType(type);
        }
        if (type != null && !type.isBlank() && !"ALL".equals(type)) {
            return contactMapper.findByUserIdAndTypeAndNameContainingOrderByCreatedAtDesc(userId, type, keyword);
        }
        return contactMapper.searchByKeyword(userId, keyword);
    }

    public Contact getById(Long id) {
        Contact existing = contactMapper.findById(id);
        if (existing == null) throw new RuntimeException("联系人不存在: " + id);
        return existing;
    }

    public Contact create(Contact contact) {
        contact.setUserId(UserContext.getUserId());
        log.info("Creating contact: {} ({})", contact.getName(), contact.getType());
        contactMapper.insert(contact);
        return contact;
    }

    public Contact update(Long id, Contact update) {
        Contact existing = getById(id);
        existing.setName(update.getName());
        existing.setType(update.getType());
        existing.setContactPerson(update.getContactPerson());
        existing.setPhone(update.getPhone());
        existing.setEmail(update.getEmail());
        existing.setAddress(update.getAddress());
        existing.setTaxNo(update.getTaxNo());
        existing.setBankName(update.getBankName());
        existing.setBankAccount(update.getBankAccount());
        existing.setRemark(update.getRemark());
        contactMapper.update(existing);
        return existing;
    }

    public void delete(Long id) {
        contactMapper.deleteById(id);
    }
}
