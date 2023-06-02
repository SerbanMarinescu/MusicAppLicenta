package com.example.aplicatielicenta.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AllSongsAdapter @Inject constructor(private val glide: RequestManager) : RecyclerView.Adapter<AllSongsAdapter.AllSongsViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    var songs: MutableList<Song> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllSongsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.song_layout, parent, false)
        return AllSongsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AllSongsViewHolder, position: Int) {
        val song = songs[position]

        holder.songName.text = song.title
        holder.songWriter.text = song.subtitle
        glide.load(song.imageUrl).into(holder.songImage)

        holder.itemView.setOnClickListener{
            onItemClickListener?.let {
                it(song)
            }
        }

    }

    override fun getItemCount(): Int {
        return songs.size
    }

    fun updateSongs(newSongs: MutableList<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }


    inner class AllSongsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

         var songImage: ImageView
         var songName: TextView
         var songWriter: TextView

        init {
            songImage = itemView.findViewById(R.id.im_song)
            songName = itemView.findViewById(R.id.tw_song_name)
            songWriter = itemView.findViewById(R.id.tw_song_writer)
        }
    }


     suspend fun getSongsFromFirestore(): List<Song> {

        return songCollection.get().await().toObjects(Song::class.java)
    }

    suspend fun fetchData() {
        songs = getSongsFromFirestore() as MutableList<Song>
        notifyDataSetChanged()
    }



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

    suspend fun getAllSongs(): List<Song>{

        val options = getRecommenderList()

        var check = false

        val genreOptions = options[0]
        val decadeOptions = options[1]
        val songWriterOptions = options[2]

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
        }.mapNotNull {
            it.toObject(Song::class.java)
        }

        return if(check){
            songs
        }
        else
        {
            songCollection.whereGreaterThanOrEqualTo("year", 2020).get().await().toObjects(Song::class.java)

        }

    }


    var onItemClickListener: ((Song) -> Unit)? = null

    fun setItemClickListener(listener: (Song) -> Unit){
        onItemClickListener = listener
    }

}