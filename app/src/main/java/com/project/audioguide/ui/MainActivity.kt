package com.project.audioguide.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.audioguide.databinding.ActivityMainBinding
import com.project.audioguide.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.project.audioguide.data.database.SightDatabase
import com.project.audioguide.data.database.sights
import android.util.Log
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Hilt provides the viewModel
    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            val dao = SightDatabase.getInstance(applicationContext).sightDao()
            dao.deleteAll()
//            dao.resetSequence()
            dao.insertAll(sights)
            val all = dao.getAll()
            Log.d("DB", "Записей: ${all.size}")
            all.forEach { Log.d("DB", "${it.name}: ${it.location.latitude} ${it.location.longitude}")}
        }
    }
}