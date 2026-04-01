package com.tfg.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tfg.engine.RiskEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class RiskResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val riskEngine: RiskEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        riskEngine.resetDailyCounters()
        Timber.i("RiskEngine daily counters reset by scheduled worker")
        return Result.success()
    }
}
