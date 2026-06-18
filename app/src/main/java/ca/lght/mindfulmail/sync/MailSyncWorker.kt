package ca.lght.mindfulmail.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MailSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MailRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return repository.sync().fold(
            onSuccess = { syncResult ->
                Log.i(TAG, "Sync complete: ${syncResult.newMessages} new, ${syncResult.updatedMessages} updated")
                Result.success()
            },
            onFailure = { error ->
                Log.w(TAG, "Sync failed: ${error.message}")
                Result.retry()
            },
        )
    }

    companion object {
        private const val TAG = "MailSyncWorker"
        private const val WORK_NAME = "mail_sync_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MailSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
