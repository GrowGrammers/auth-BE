package com.wq.demo.entity

import jakarta.persistence.*

@Entity
open class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,
    open val name: String = ""
)