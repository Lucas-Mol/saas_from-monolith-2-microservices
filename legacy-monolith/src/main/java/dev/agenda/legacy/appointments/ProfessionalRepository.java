package dev.agenda.legacy.appointments;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    List<Professional> findByActiveTrueOrderByFullNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Professional p where p.id = :id")
    Optional<Professional> findByIdForUpdate(@Param("id") Long id);
}
