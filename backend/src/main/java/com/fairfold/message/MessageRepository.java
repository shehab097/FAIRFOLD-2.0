package com.fairfold.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByApplicationIdOrderByCreatedAtAscIdAsc(UUID applicationId);

    @Modifying
    @Query("update Message m set m.readAt = :now where m.applicationId = :applicationId "
            + "and m.senderId <> :readerId and m.readAt is null")
    int markReadFor(@Param("applicationId") UUID applicationId,
                    @Param("readerId") UUID readerId,
                    @Param("now") Instant now);
}
