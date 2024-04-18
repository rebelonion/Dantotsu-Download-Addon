package ani.dantotsu.downloadAddon

import android.content.Context
import android.net.Uri
import android.util.Log
import ani.dantotsu.addons.download.DownloadAddonApi
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.SessionState

class DownloadAddon : DownloadAddonApi {
    override fun cancelDownload(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }

    override fun setDownloadPath(context: Context, uri: Uri): String {
        return FFmpegKitConfig.getSafParameterForWrite(
            context,
            uri
        )
    }

    override suspend fun executeFFProbe(request: String, logCallback: (String) -> Unit) {
        FFprobeKit.executeAsync(
            request,
            {
                Log.d("FFmpegKit", it.allLogsAsString)
            }, {
                if (it.message.toDoubleOrNull() != null) {
                    logCallback(it.message)
                }
            })
    }

    override suspend fun executeFFMpeg(request: String, statCallback: (Double) -> Unit): Long {
        val exec = FFmpegKit.executeAsync(request,
            { session ->
                val state: SessionState = session.state
                val returnCode = session.returnCode
                // CALLED WHEN SESSION IS EXECUTED
                Log.d("FFmpegKit",
                    java.lang.String.format(
                        "FFmpeg process exited with state %s and rc %s.%s",
                        state,
                        returnCode,
                        session.failStackTrace
                    )
                )

            }, {
                // CALLED WHEN SESSION PRINTS LOGS
                Log.d("FFmpegKit", it.message)
            }) {
            statCallback(it.time)
            Log.d("FFmpegKit", "Statistics: $it")
        }
        return exec.sessionId
    }

    override fun getState(sessionId: Long): String {
        FFmpegKitConfig.getFFmpegSessions().forEach {
            if (it.sessionId == sessionId) {
                return when (it.state) {
                    SessionState.COMPLETED -> "COMPLETED"
                    SessionState.FAILED -> "FAILED"
                    SessionState.RUNNING -> "RUNNING"
                    else -> "UNKNOWN"
                }
            }
        }
        return "UNKNOWN"
    }

    override fun getStackTrace(sessionId: Long): String? {
        FFmpegKitConfig.getFFmpegSessions().forEach {
            if (it.sessionId == sessionId) {
                return it.failStackTrace
            }
        }
        return null
    }

    override fun hadError(sessionId: Long): Boolean {
        FFmpegKitConfig.getFFmpegSessions().forEach {
            if (it.sessionId == sessionId) {
                return it.returnCode.isValueError
            }
        }
        return false
    }
}