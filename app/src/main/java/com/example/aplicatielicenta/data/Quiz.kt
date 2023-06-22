package com.example.aplicatielicenta.data

class Quiz {

    val quizQuestions = listOf(
        "What is your primary purpose for using this app?",
        "What genre of music do you enjoy the most?",
        "Do you prefer instrumental music or music with lyrics?",
        "Who is your favorite music artist?",
        // Add more questions here
    )

    val quizOptions = listOf(
        listOf("Workout and fitness", "Office and productivity", "Homework and studying", "Leisure and relaxation"),
        listOf("Pop", "Romantic", "Dance", "Country"),
        listOf("Instrumental music only", "Music with lyrics only", "I enjoy both instrumental music and music with lyrics equally", "I don't have a preference"),
        listOf("Andia", "Ed Sheeran", "The Weeknd", "Ellie Goulding")
        // Add more options here
    )

}