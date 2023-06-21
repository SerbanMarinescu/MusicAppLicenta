package com.example.aplicatielicenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.aplicatielicenta.data.Quiz
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class QuizActivity : AppCompatActivity() {

    private lateinit var question: TextView
    private lateinit var option1: CheckBox
    private lateinit var option2: CheckBox
    private lateinit var option3: CheckBox
    private lateinit var option4: CheckBox
    private lateinit var submitBtn: Button
    private lateinit var layoutCheckBox: LinearLayout
    private lateinit var radioGroup: RadioGroup
    private lateinit var account: FirebaseAuth

    private val quiz: Quiz = Quiz()
    private var questionIndex = 0
    private var count = 1
    private val size = quiz.quizQuestions.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        question = findViewById(R.id.twQuestion)
        option1 = findViewById(R.id.Option1)
        option2 = findViewById(R.id.Option2)
        option3 = findViewById(R.id.Option3)
        option4 = findViewById(R.id.Option4)
        submitBtn = findViewById(R.id.btnSubmit)
        layoutCheckBox = findViewById(R.id.layout_options)
        radioGroup = findViewById(R.id.rgOptions)
        account = FirebaseAuth.getInstance()


        val listOption = ArrayList<CheckBox>(listOf(option1, option2, option3, option4))

        createQuiz()

        submitBtn.setOnClickListener{

            val userRef = FirebaseDatabase.getInstance().reference.child("Quiz").child(account.currentUser!!.uid)

            if(count == 3){

                var radioGroupOption = ""
                val selectedOptionId = radioGroup.checkedRadioButtonId
                val radioButton: RadioButton = findViewById(selectedOptionId)
                radioGroupOption = radioButton.text.toString()

                userRef.child("Question $count").setValue(radioGroupOption)

            }
            else {
                val selectedOptions = listOption.filter {
                    it.isChecked
                }.map {
                    it.text.toString()
                }


                userRef.child("Question $count").setValue(selectedOptions)
            }



            questionIndex++
            count++

            createQuiz()

            if(questionIndex == size){
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun createQuiz() {

        if(questionIndex < size){
            question.text = "Question [$count/$size] ${quiz.quizQuestions[questionIndex]}"

            if(count == 3){
                radioGroup.visibility = View.VISIBLE
                layoutCheckBox.visibility = View.GONE


            }
            else{
                radioGroup.visibility = View.GONE
                layoutCheckBox.visibility = View.VISIBLE

                option1.text = quiz.quizOptions[questionIndex][0]
                option2.text = quiz.quizOptions[questionIndex][1]
                option3.text = quiz.quizOptions[questionIndex][2]
                option4.text = quiz.quizOptions[questionIndex][3]

                option1.isChecked = false
                option2.isChecked = false
                option3.isChecked = false
                option4.isChecked = false
            }


        }
    }
}