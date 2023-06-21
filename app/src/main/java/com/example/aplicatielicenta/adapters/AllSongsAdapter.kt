package com.example.aplicatielicenta.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.data.Song
import com.example.aplicatielicenta.other.Constants.SONG_COLLECTION
import com.example.aplicatielicenta.other.PlaylistClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AllSongsAdapter @Inject constructor(private val glide: RequestManager,
                                          private val playlistClickListener: PlaylistClickListener
) : RecyclerView.Adapter<AllSongsAdapter.AllSongsViewHolder>() {

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

        getLikes(song.mediaId, holder.likeBtn, song)


        holder.playlistBtn.setOnClickListener{
            showPlaylistOptionsDialog(holder.itemView.context, song)

        }

        holder.likeBtn.setOnClickListener{

            if(!song.isLiked){
                holder.likeBtn.setImageResource(R.drawable.heart_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).setValue(true)
                song.isLiked = true
            }
            else{
                holder.likeBtn.setImageResource(R.drawable.heart_not_filled)
                FirebaseDatabase.getInstance().reference.child("Liked")
                    .child(FirebaseAuth.getInstance().currentUser!!.uid).child(song.mediaId).removeValue()
                song.isLiked = false
            }
        }

        holder.itemView.setOnClickListener{
            onItemClickListener?.let {
                it(song)
            }
        }

    }

    private fun showPlaylistOptionsDialog(context: Context, song: Song) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add to Playlist")
            .setPositiveButton("Create New Playlist") { dialog, _ ->
                playlistClickListener.onCreateNewPlaylistClicked(song)
                dialog.dismiss()
            }
            .setNegativeButton("Add to Existing Playlist") { dialog, _ ->
                playlistClickListener.onAddToExistingPlaylistClicked(song)
                dialog.dismiss()
            }


        dialog.show()
    }


    private fun getLikes(mediaId: String, likeBtn: ImageView, song: Song) {

        val likeRef = FirebaseDatabase.getInstance().reference.child("Liked")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        likeRef.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(mediaId).exists()){
                    likeBtn.setImageResource(R.drawable.heart_filled)
                    song.isLiked = true
                }
                else{
                    likeBtn.setImageResource(R.drawable.heart_not_filled)
                    song.isLiked = false
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
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
         var likeBtn: ImageView
         val playlistBtn: ImageView

        init {
            songImage = itemView.findViewById(R.id.im_song)
            songName = itemView.findViewById(R.id.tw_song_name)
            songWriter = itemView.findViewById(R.id.tw_song_writer)
            likeBtn = itemView.findViewById(R.id.btn_like)
            playlistBtn = itemView.findViewById(R.id.btn_playlist)
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

                    if(snapshot.exists()){
                        if(snapshot.hasChildren()){
                            for (snap in snapshot.children) {
                                val value = snap.getValue(String::class.java)
                                value?.let {
                                    optionsList.add(value)
                                }
                            }
                        }
                        else{
                            val value = snapshot.getValue(String::class.java)
                            value?.let {
                                optionsList.add(value)
                            }
                        }
                    }
                    else{
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

        val questions = listOf("Question 1", "Question 2", "Question 3", "Question 4")

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

        val purposeOptions = options[0]
        val genreOptions = options[1]
        val typeOptions = options[2]
        val songWriterOptions = options[3]

        var purposeQuery: Query = songCollection
        var genreQuery: Query = songCollection
        var typeQuery: Query = songCollection
        var songWriterQuery: Query = songCollection

        val purposeSnapshot: QuerySnapshot
        val genreSnapshot: QuerySnapshot
        val typeSnapshot: QuerySnapshot
        val songwriterSnapshot: QuerySnapshot

        val mergedSnapshots = mutableListOf<DocumentSnapshot>()

        if(!purposeOptions.contains("NoneSelected")){
            purposeQuery = purposeQuery.whereIn("purpose", purposeOptions)
            purposeSnapshot = purposeQuery.get().await()
            mergedSnapshots.addAll(purposeSnapshot.documents)
            check = true
        }

        if(!genreOptions.contains("NoneSelected")){
            genreQuery = genreQuery.whereIn("genre", genreOptions)
            genreSnapshot = genreQuery.get().await()
            mergedSnapshots.addAll(genreSnapshot.documents)
            check = true
        }

//        if(!typeOptions.contains("I don't have a preference")){
//
//            typeQuery = typeQuery.whereIn("type", typeOptions)
//            typeSnapshot = typeQuery.get().await()
//            mergedSnapshots.addAll(typeSnapshot.documents)
//            check = true
//        }

        when{
            typeOptions.contains("Instrumental music only") -> {
                typeQuery = typeQuery.whereEqualTo("type", "Instrumental")
                typeSnapshot = typeQuery.get().await()
                mergedSnapshots.addAll(typeSnapshot.documents)
                check = true
            }

            typeOptions.contains("Music with lyrics only") -> {
                typeQuery = typeQuery.whereEqualTo("type", "Lyrics")
                typeSnapshot = typeQuery.get().await()
                mergedSnapshots.addAll(typeSnapshot.documents)
                check = true
            }

            else -> Unit
        }


        if(!songWriterOptions.contains("NoneSelected")){
            songWriterQuery = songWriterQuery.whereIn("subtitle", songWriterOptions)
            songwriterSnapshot = songWriterQuery.get().await()
            mergedSnapshots.addAll(songwriterSnapshot.documents)
            check = true
        }

//        Log.d("Type", purposeOptions.toString())
//        Log.d("Type", genreOptions.toString())
//        Log.d("Type", typeOptions.toString())
//        Log.d("Type", songWriterOptions.toString())

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