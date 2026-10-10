package dev.agenda.legacy.appointments;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    List<Appointment> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAscIdAsc(
            LocalDateTime from, LocalDateTime to);

    List<Appointment> findByProfessionalIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAscIdAsc(
            Long professionalId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select a from Appointment a
            where a.professionalId = :professionalId
              and a.status <> dev.agenda.legacy.appointments.AppointmentStatus.CANCELLED
              and a.startTime < :end
              and a.endTime > :start
            order by a.startTime
            """)
    List<Appointment> findActiveOverlapping(@Param("professionalId") Long professionalId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("""
            select a from Appointment a
            where a.customerId = :customerId
              and a.status = dev.agenda.legacy.appointments.AppointmentStatus.COMPLETED
              and not exists (select l.id from InvoiceLine l where l.appointmentId = a.id)
            order by a.startTime, a.id
            """)
    List<Appointment> findCompletedNotInvoiced(@Param("customerId") Long customerId);
}
