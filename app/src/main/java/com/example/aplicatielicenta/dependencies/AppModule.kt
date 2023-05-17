package com.example.aplicatielicenta.dependencies

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.aplicatielicenta.R
import com.example.aplicatielicenta.adapters.SwipeSongAdapter
import com.example.aplicatielicenta.exoplayer.MusicServiceConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideMusicServiceConnection(@ApplicationContext context: Context) = MusicServiceConnection(context)

    @Singleton
    @Provides
    fun providesSwipeSongAdapter() = SwipeSongAdapter()

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.music_template)
            .error(R.drawable.music_template)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )

}