package com.ape.longtimer

import android.app.AlertDialog
import android.app.appsearch.SearchResult.MatchInfo
import android.content.DialogInterface
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.annotation.RawRes
import com.ape.longtimer.Utils.Companion.timeStringFromMillis
import com.ape.longtimer.adapter.SegmentItemAdapter
import com.ape.longtimer.databinding.ActivityMainBinding
import com.ape.longtimer.model.Segment
import com.google.android.material.button.MaterialButton
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var dataHelper: DataHelper

    private val mediaPlayer = MediaPlayer().apply {
        setOnPreparedListener { start() }
        setOnCompletionListener { reset() }
    }

    companion object {
        const val TIMER_RESOLUTION_MS: Long = 100
    }

    private val timer = Timer()
    private val segmentsList = SegmentsList(mutableListOf())

    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dataHelper = DataHelper(applicationContext)

        binding.segmentsView.adapter = SegmentItemAdapter(this, segmentsList.getAll())

        binding.startButton.setOnClickListener{ startStopAction() }
        binding.stopButton.setOnClickListener{ startStopAction() }
        binding.resetButton.setOnClickListener{ resetAction() }

        binding.addSegmentButton.setOnClickListener{
            val name = binding.newSegmentText.text.toString()
            val time = binding.newSegmentTime.text.toString().toIntOrNull()
            if (time != null) {
                addSegment(name, time * 60 )  // UI asks for minutes, addSegment takes seconds
            }
            binding.newSegmentText.setText("")
            binding.newSegmentTime.setText("")
            Utils.hideSoftKeyboard(this)
            binding.segmentsView.adapter!!.notifyItemInserted(segmentsList.getSize() - 1)
        }

        binding.timeTravelButton.setOnClickListener{
            val input = EditText(this)
            input.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED

            val builder = AlertDialog.Builder(this)
                .setTitle("Time Travel")
                .setMessage("Travel by how many minutes into the future? (Use a negative number to travel into the past.)")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val startTime = dataHelper.startTime()
                    try {
                        val mins = input.text.toString().toInt().minutes
                        if (startTime != null) {
                            dataHelper.setStartTime(Date(startTime.time - mins.inWholeMilliseconds))
                        }
                    } catch (e: NumberFormatException) {
                        // Ignore, do nothing
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> }

            builder.show()
        }

        if (dataHelper.timerCounting()) {
            startTimer()
        } else {
            stopTimer()
            if (dataHelper.startTime() != null && dataHelper.stopTime() != null) {
                val time = Date().time - calculateRestartTime().time
                binding.timeText.text = timeStringFromMillis(time)
            }
        }
        updateActiveSegment()

        timer.scheduleAtFixedRate(TimeTask(), 0, TIMER_RESOLUTION_MS)

        binding.currentSegment.text =
            getString(R.string.current_segment, getString(R.string.not_running))
    }

    private inner class TimeTask: TimerTask() {
        override fun run() {
            if (dataHelper.timerCounting()) {
                val time = currentTimerMillis()
                val timeSecs = currentTimerSecs()
                binding.timeText.text = timeStringFromMillis(time)
                if (time < TIMER_RESOLUTION_MS ||  // opening bell
                    (time % 1000 < TIMER_RESOLUTION_MS && segmentsList.shouldRingAt(timeSecs))) {
                    playSound(R.raw.bell)
                    updateActiveSegment()
                }
                if (timeSecs > segmentsList.getTotalDurationSecs()) {
                    // Session is done
                    updateActiveSegment()
                }
            }
        }
    }

    private fun startStopAction() {
        if (dataHelper.timerCounting()) {
            dataHelper.setStopTime(Date())
            stopTimer()
        } else {
            if (dataHelper.stopTime() != null) {
                dataHelper.setStartTime(calculateRestartTime())
                dataHelper.setStopTime(null)
            } else {
                dataHelper.setStartTime(Date())
            }
            startTimer()
        }
        updateActiveSegment()
    }

    private fun currentTimerMillis(): Long {
        // Assumes timer is correctly initialized from new session or restored from previous
        // session. Do not call from onCreate().
        return Date().time - dataHelper.startTime()!!.time
    }

    private fun currentTimerSecs(): Int {
        // Assumes timer is correctly initialized from new session or restored from previous
        // session. Do not call from onCreate().
        return (currentTimerMillis() / 1000).toInt()
    }

    private fun calculateRestartTime(): Date {
        val diff = dataHelper.startTime()!!.time - dataHelper.stopTime()!!.time
        return Date(System.currentTimeMillis() + diff)
    }

    private fun resetAction() {
        dataHelper.setStopTime(null)
        dataHelper.setStartTime(null)
        stopTimer()
        binding.timeText.text = timeStringFromMillis(0)
        updateActiveSegment()
    }

    private fun startTimer() {
        if (segmentsList.getSize() == 0) return  // don't start timer if the session is empty
        dataHelper.setTimerCounting(true)
        disableButton(binding.startButton)
        enableButton(binding.stopButton)
        disableButton(binding.resetButton)
        enableButton(binding.timeTravelButton)
    }

    private fun stopTimer() {
        dataHelper.setTimerCounting(false)
        enableButton(binding.startButton)
        disableButton(binding.stopButton)
        enableButton(binding.resetButton)
        disableButton(binding.timeTravelButton)
    }

    private fun enableButton(button: MaterialButton) {
        button.isClickable = true
        button.alpha = 1.0F
    }

    private fun disableButton(button: MaterialButton) {
        button.isClickable = false
        button.alpha = 0.4F
    }

    private fun playSound(@RawRes rawResId: Int) {
        // If playSound() only supports one sound at a time. If it is called while a previous
        // call's sound is playing, the previous sound terminates early and the new sound begins
        // playing.
        val assetFileDescriptor = this.resources.openRawResourceFd(rawResId)
        mediaPlayer.run {
            reset()
            setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength)
            prepareAsync()
        }
        assetFileDescriptor.close()
    }

    private fun addSegment(name: String, time: Int) {
        segmentsList.add(Segment(name, time))
    }

    private fun updateActiveSegment() {
        val activeSegment =
            if (dataHelper.timerCounting()) segmentsList.getActiveSegmentIndex(currentTimerSecs())
            else -1
        val numSegments = segmentsList.getSize()

        for (i in 0 until numSegments) {
            segmentsList.get(i).isCurrent = (i == activeSegment)
        }

        if (activeSegment != -1 && activeSegment < numSegments) {
            val currentSegment = segmentsList.get(activeSegment)
            binding.currentSegment.text = getString(R.string.current_segment, currentSegment.name)
        } else if (activeSegment == -1) {
            // Not yet started or otherwise paused
            binding.currentSegment.text = getString(
                R.string.current_segment, getString(R.string.not_running))
        } else {
            // Timer is done
            binding.currentSegment.text = getString(R.string.session_complete)
            stopTimer()
        }
    }
}