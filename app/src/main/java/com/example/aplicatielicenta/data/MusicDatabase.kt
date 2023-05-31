package com.example.aplicatielicenta.data

import android.util.Log
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MusicDatabase {

    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)


    suspend fun fetchOptions(questionRef: DatabaseReference): List<String> {

        return suspendCoroutine { continuation ->

            questionRef.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val optionsList = mutableListOf<String>()

                    if (snapshot.exists()) {
                        for (snap in snapshot.children) {
                            val value = snap.getValue(String::class.java)
                            value?.let {
                                optionsList.add(value)
                            }
                        }
                    } else {
                        optionsList.add("NoneSelected")
                    }

                    continuation.resume(optionsList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error if necessary
                    continuation.resume(emptyList())
                }
            })
        }
    }


    suspend fun getRecommenderList(): List<List<String>> = coroutineScope{

        val recommenderList: MutableList<List<String>> = mutableListOf()

        val quizRef = FirebaseDatabase.getInstance().reference.child("Quiz")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        val questions = listOf("Question 1", "Question 2", "Question 3")

        for (question in questions) {
            val questionRef = quizRef.child(question)

            val optionsList = async {
                fetchOptions(questionRef)
            }
            recommenderList.add(optionsList.await())
        }
        return@coroutineScope recommenderList
    }



    suspend fun getAllSongs(): List<Song> = coroutineScope {

        return@coroutineScope try {

            val options = getRecommenderList()

            delay(5000)

            val genreOptions = options[0]
            val decadeOptions = options[1]
            val songWriterOptions = options[2]

            var check = false

            var genreQuery: Query = songCollection
            var decadeQuery: Query = songCollection
            var songWriterQuery: Query = songCollection

            val genreSnapshot: QuerySnapshot
            val decadeSnapshot: QuerySnapshot
            val songwriterSnapshot: QuerySnapshot

            val mergedSnapshots = mutableListOf<DocumentSnapshot>()


            if(!genreOptions.contains("NoneSelected")){
                genreQuery = genreQuery.whereIn("genre", genreOptions)
                genreSnapshot = genreQuery.get().await()
                mergedSnapshots.addAll(genreSnapshot.documents)
                check = true
            }


            if(!decadeOptions.contains("NoneSelected")){

                val sortedDecades = decadeOptions.map {
                    it.toInt()
                }.sorted()


                decadeQuery = decadeQuery.whereLessThanOrEqualTo("year", sortedDecades.last())
                    .whereGreaterThanOrEqualTo("year", sortedDecades.first())
                decadeSnapshot = decadeQuery.get().await()
                mergedSnapshots.addAll(decadeSnapshot.documents)
                check = true
            }


            if(!songWriterOptions.contains("NoneSelected")){
                songWriterQuery = songWriterQuery.whereIn("subtitle", songWriterOptions)
                songwriterSnapshot = songWriterQuery.get().await()
                mergedSnapshots.addAll(songwriterSnapshot.documents)
                check = true
            }



            val songs = mergedSnapshots.distinctBy {
                it.id
            }.map {
                it.toObject(Song::class.java)
            }


            (if(check){
                songs
            } else{
                 songCollection.get().await().toObjects(Song::class.java)
            }) as List<Song>


        } catch (e: Exception) {
            emptyList()
        }
    }











}