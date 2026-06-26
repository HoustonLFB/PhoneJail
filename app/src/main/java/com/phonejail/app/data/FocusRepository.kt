package com.phonejail.app.data

import kotlinx.coroutines.flow.Flow

/** Thin layer over the DAO so the rest of the app never touches Room directly. */
class FocusRepository(private val dao: FocusSessionDao) {

    val sessions: Flow<List<FocusSession>> = dao.observeAll()

    suspend fun record(session: FocusSession): Long = dao.insert(session)

    suspend fun delete(id: Long) = dao.delete(id)
}
