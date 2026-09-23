package com.optiontracker.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val POSITIONS = "positions"
    const val ASSIGNED = "assigned"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{positionId}"
    const val EDITOR = "editor?positionId={positionId}"
    const val CLOSE = "close/{positionId}?assigned={assigned}"
    const val ASSIGNED_EDITOR = "assignedEditor?lotId={lotId}"
    const val HISTORY_DETAIL = "historyDetail/{positionId}"

    fun detail(id: Long) = "detail/$id"
    fun editor(id: Long? = null) = "editor?positionId=${id ?: -1L}"
    fun close(id: Long, assigned: Boolean = false) = "close/$id?assigned=$assigned"
    fun assignedEditor(id: Long? = null) = "assignedEditor?lotId=${id ?: -1L}"
    fun historyDetail(id: Long) = "historyDetail/$id"
}
