package travisfw.unbirday.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import travisfw.unbirday.model.Event


@Dao
interface EventDao {
    // Replace on conflict. This means that contacts will have priority over Birday data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEventReplace(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllEventReplace(events: List<Event>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateEventReplace(event: Event)

    // Ignore on conflict. This means that Birday will have priority over contacts data
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEventIgnore(event: Event)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllEventIgnore(events: List<Event>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun updateEventIgnore(event: Event)

    @Delete
    fun deleteEvent(event: Event)

    @Delete
    fun deleteAllEvent(event: List<Event>)

    // All events (LiveData). nextDate computation is done in Kotlin.
    @Query("SELECT * FROM Event")
    fun getEvents(): LiveData<List<Event>>

    @Query("SELECT COUNT(id) FROM Event")
    fun getEventsCount(): LiveData<Int>

    // All events, unordered. Sorting by nextDate is done in Kotlin.
    @Query("SELECT * FROM Event")
    fun getAllEvents(): LiveData<List<Event>>

    // All events, static (non-LiveData) for exports and workers
    @Query("SELECT * FROM Event")
    fun getAllEventsStatic(): List<Event>

    // Search by name/surname
    @Query("SELECT * FROM Event WHERE name || ' ' || surname LIKE '%' || :searchString || '%'")
    fun getEventsByName(searchString: String): LiveData<List<Event>>

    // Filter by event type
    @Query("SELECT * FROM Event WHERE type = :selectedType")
    fun getEventsByType(selectedType: String): LiveData<List<Event>>

    // Favorite events
    @Query("SELECT * FROM Event WHERE favorite = 1")
    fun getFavoriteEvents(): LiveData<List<Event>>

    // Special age events (filtering done in Kotlin now since nextDate is computed there)
    @Query("SELECT * FROM Event WHERE type = :type")
    fun getEventsByTypeStatic(type: String): List<Event>

    // Events whose effective DOW (COALESCE of explicit override or derived from originalDate)
    // and day-of-month match the given values. Both arguments use SQLite's 0=Sun…6=Sat scale.
    @Query("""
        SELECT * FROM Event
        WHERE CAST(strftime('%d', originalDate) AS INTEGER) = :dayOfMonth
        AND COALESCE(dayOfWeek, CAST(strftime('%w', originalDate) AS INTEGER)) = :dayOfWeek
    """)
    fun getEventsForUnbirday(dayOfWeek: Int, dayOfMonth: Int): List<Event>

    // Checkpoint functionality, not yet supported in room but useful to avoid closing the db during the backup creation
    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}
