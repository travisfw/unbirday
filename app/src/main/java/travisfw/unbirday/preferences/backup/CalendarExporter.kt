package travisfw.unbirday.preferences.backup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import travisfw.unbirday.R
import travisfw.unbirday.activities.MainActivity
import travisfw.unbirday.model.EventResult
import travisfw.unbirday.utilities.addEvent
import travisfw.unbirday.utilities.createOrGetCalendar
import travisfw.unbirday.utilities.deleteLocalCalendar
import travisfw.unbirday.utilities.formatName
import travisfw.unbirday.utilities.formatUnbirdayForEvent
import travisfw.unbirday.utilities.getEffectiveDayOfWeek
import travisfw.unbirday.utilities.getStringForTypeCodename
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.concurrent.thread


class CalendarExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener, View.OnLongClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
        v.setOnLongClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        v.setOnClickListener(null)
        act.vibrate()
        act.showLoadingIndicator()
        // Only export if there's at least one event
        if (act.mainViewModel.allEventsUnfiltered.value.isNullOrEmpty()) {
            act.showSnackbar(context.getString(R.string.no_events))
        } else {
            thread {
                exportCalendar(context)
                (context as MainActivity).runOnUiThread {
                    v.setOnClickListener(this)
                    act.hideLoadingIndicator()
                }
            }
        }
    }

    // Import the contacts from device contacts (not necessarily Google)
    fun exportCalendar(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for read / write calendar permissions
        val permissionWrite = act.askWriteCalendarPermission(402)
        if (!permissionWrite) return false
        val permissionRead = act.askCalendarPermission(302)
        if (!permissionRead) return false

        // Phase 1: create the Birday calendar
        var calendarId = createOrGetCalendar(act)
        if (calendarId == -1L) calendarId = createOrGetCalendar(act)
        if (calendarId == -1L) {
            context.runOnUiThread { context.showSnackbar(context.getString(R.string.birday_export_failure)) }
            return false
        }

        // Phase 2: get every event and write it the the system calendar
        val events = act.mainViewModel.allEventsUnfiltered.value
        if (events.isNullOrEmpty()) {
            context.runOnUiThread {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            }
        }
        val writeOk = writeEventsToCalendar(act, events!!, calendarId)

        // Phase 3: check if the write event went as expected and return accordingly
        return if (writeOk) {
            context.runOnUiThread { context.showSnackbar(context.getString(R.string.birday_export_success)) }
            true
        } else {
            context.runOnUiThread { context.showSnackbar(context.getString(R.string.birday_export_failure)) }
            false
        }

    }

    // Write each event as individual non-recurring entries for all unbirday occurrences in 2 years
    private fun writeEventsToCalendar(
        context: Context,
        events: List<EventResult>,
        calendarId: Long
    ): Boolean {
        try {
            val today = LocalDate.now()
            val endDate = today.plusYears(2)

            for (event in events) {
                val dayOfWeek = getEffectiveDayOfWeek(event)
                val dayOfMonth = event.originalDate.dayOfMonth
                val unbirdayLabel = formatUnbirdayForEvent(event)
                val eventName = formatName(event, false) + " - ${
                    getStringForTypeCodename(context, event.type!!)
                } ($unbirdayLabel)"

                // Find all matching dates in the next 2 years
                var candidate = today
                while (candidate.isBefore(endDate)) {
                    if (candidate.dayOfMonth == dayOfMonth && candidate.dayOfWeek == dayOfWeek) {
                        val startMillis = candidate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                        addEvent(context, calendarId, eventName, event.notes, startMillis)
                    }
                    candidate = candidate.plusDays(1)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Delete the local calendar on long click
    override fun onLongClick(p0: View?): Boolean {
        val act = context as MainActivity
        act.vibrate()
        deleteLocalCalendar(context, context.getString(R.string.events_notification_channel))
        act.showSnackbar(context.getString(R.string.deleted))
        return true
    }
}