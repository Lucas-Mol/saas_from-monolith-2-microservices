package dev.agenda.legacy.appointments;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final ProfessionalRepository professionalRepository;

    public ProfessionalController(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Professional> list() {
        return professionalRepository.findByActiveTrueOrderByFullNameAsc();
    }
}
