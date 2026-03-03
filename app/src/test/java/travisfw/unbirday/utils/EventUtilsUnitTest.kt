package travisfw.unbirday.utils

import travisfw.unbirday.model.Event
import travisfw.unbirday.model.EventResult
import travisfw.unbirday.utilities.getEffectiveDayOfWeek
import travisfw.unbirday.utilities.sqliteValue
import travisfw.unbirday.utilities.toDayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DayOfWeekConventionTest {

    // Known fixed dates: 2024-01-01=Monday, 2024-01-07=Sunday
    private val monday = LocalDate.of(2024, 1, 1)
    private val sunday = LocalDate.of(2024, 1, 7)

    // --- Int.toDayOfWeek() ---

    @Test fun toDayOfWeek_0_is_sunday()    = assertEquals(DayOfWeek.SUNDAY,    0.toDayOfWeek())
    @Test fun toDayOfWeek_1_is_monday()    = assertEquals(DayOfWeek.MONDAY,    1.toDayOfWeek())
    @Test fun toDayOfWeek_2_is_tuesday()   = assertEquals(DayOfWeek.TUESDAY,   2.toDayOfWeek())
    @Test fun toDayOfWeek_3_is_wednesday() = assertEquals(DayOfWeek.WEDNESDAY, 3.toDayOfWeek())
    @Test fun toDayOfWeek_4_is_thursday()  = assertEquals(DayOfWeek.THURSDAY,  4.toDayOfWeek())
    @Test fun toDayOfWeek_5_is_friday()    = assertEquals(DayOfWeek.FRIDAY,    5.toDayOfWeek())
    @Test fun toDayOfWeek_6_is_saturday()  = assertEquals(DayOfWeek.SATURDAY,  6.toDayOfWeek())

    // --- DayOfWeek.sqliteValue ---

    @Test fun sqliteValue_sunday_is_0()    = assertEquals(0, DayOfWeek.SUNDAY.sqliteValue)
    @Test fun sqliteValue_monday_is_1()    = assertEquals(1, DayOfWeek.MONDAY.sqliteValue)
    @Test fun sqliteValue_tuesday_is_2()   = assertEquals(2, DayOfWeek.TUESDAY.sqliteValue)
    @Test fun sqliteValue_wednesday_is_3() = assertEquals(3, DayOfWeek.WEDNESDAY.sqliteValue)
    @Test fun sqliteValue_thursday_is_4()  = assertEquals(4, DayOfWeek.THURSDAY.sqliteValue)
    @Test fun sqliteValue_friday_is_5()    = assertEquals(5, DayOfWeek.FRIDAY.sqliteValue)
    @Test fun sqliteValue_saturday_is_6()  = assertEquals(6, DayOfWeek.SATURDAY.sqliteValue)

    // --- Round-trip: every DayOfWeek survives sqliteValue → toDayOfWeek ---

    @Test
    fun round_trip_all_days() {
        for (dow in DayOfWeek.entries) {
            assertEquals("Round-trip failed for $dow", dow, dow.sqliteValue.toDayOfWeek())
        }
    }

    // --- getEffectiveDayOfWeek(Event) ---

    private fun makeEvent(dayOfWeek: Int?, originalDate: LocalDate) =
        Event(id = 0, name = "Test", originalDate = originalDate, dayOfWeek = dayOfWeek)

    @Test
    fun effectiveDow_event_explicit_sunday_sqlite_0() {
        // Stored as 0 (SQLite Sunday); originalDate is a Monday — override must win
        assertEquals(DayOfWeek.SUNDAY, getEffectiveDayOfWeek(makeEvent(0, monday)))
    }

    @Test
    fun effectiveDow_event_explicit_monday_sqlite_1() {
        // Stored as 1 (Monday); originalDate is a Sunday — override must win
        assertEquals(DayOfWeek.MONDAY, getEffectiveDayOfWeek(makeEvent(1, sunday)))
    }

    @Test
    fun effectiveDow_event_null_derives_monday_from_original() {
        assertEquals(DayOfWeek.MONDAY, getEffectiveDayOfWeek(makeEvent(null, monday)))
    }

    @Test
    fun effectiveDow_event_null_derives_sunday_from_original() {
        assertEquals(DayOfWeek.SUNDAY, getEffectiveDayOfWeek(makeEvent(null, sunday)))
    }

    // --- getEffectiveDayOfWeek(EventResult) ---

    private fun makeEventResult(dayOfWeek: Int?, originalDate: LocalDate) =
        EventResult(id = 0, name = "Test", originalDate = originalDate, dayOfWeek = dayOfWeek)

    @Test
    fun effectiveDow_result_explicit_sunday_sqlite_0() {
        assertEquals(DayOfWeek.SUNDAY, getEffectiveDayOfWeek(makeEventResult(0, monday)))
    }

    @Test
    fun effectiveDow_result_explicit_monday_sqlite_1() {
        assertEquals(DayOfWeek.MONDAY, getEffectiveDayOfWeek(makeEventResult(1, sunday)))
    }

    @Test
    fun effectiveDow_result_null_derives_monday_from_original() {
        assertEquals(DayOfWeek.MONDAY, getEffectiveDayOfWeek(makeEventResult(null, monday)))
    }

    @Test
    fun effectiveDow_result_null_derives_sunday_from_original() {
        assertEquals(DayOfWeek.SUNDAY, getEffectiveDayOfWeek(makeEventResult(null, sunday)))
    }
}
