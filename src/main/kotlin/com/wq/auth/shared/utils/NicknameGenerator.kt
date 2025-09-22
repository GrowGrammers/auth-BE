package com.wq.auth.shared.utils

import org.springframework.stereotype.Component

@Component
class NicknameGenerator {
    private val adjectives = listOf(
        "귀여운", "멋진", "용감한", "재빠른", "행복한",
        "슬기로운", "웃는", "반짝이는", "든든한", "똑똑한"
    )

    private val animals = listOf(
        "토끼", "사자", "호랑이", "여우", "곰",
        "펭귄", "돌고래", "부엉이", "고양이", "강아지"
    )

    fun generate(): String {
        val adj = adjectives.random()
        val animal = animals.random()
        val number = (100..999).random()
        return "$adj$animal$number"
    }
}