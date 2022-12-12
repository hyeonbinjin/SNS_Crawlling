package com.company.sns_crawlling

import android.os.Handler
import android.os.Looper
import android.os.Message


abstract class ThreadTask<T1, T2> : Runnable {
    lateinit var mArgument: Array<T1>
    var mResult : T2? = null

    // Handle the result
    val WORK_DONE = 0
    var mResultHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            // Call onPostExecute
            mResult?.let { onPostExecute(it) }
        }
    }

    // Execute
    fun execute(vararg arg : T1) {
        // Store the argument
        mArgument = arg as Array<T1>

        // Call onPreExecute
        onPreExecute()

        // Begin thread work
        val thread = Thread(this)
        thread.start()
    }

    override fun run() {
        // Call doInBackground
        mResult = doInBackground(*mArgument)

        // Notify main thread that the work is done
        mResultHandler.sendEmptyMessage(WORK_DONE)
    }

    // onPreExecute
    protected abstract fun onPreExecute();

    // doInBackground
    protected abstract fun doInBackground(vararg arg : T1) : T2;

    // onPostExecute
    protected abstract fun onPostExecute(result : T2);
}