package com.wq.demo.email

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthEmailRepository : JpaRepository<EmailVerification, String> {

}