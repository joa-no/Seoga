package com.joano.seoga.ui.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object SearchAdd : Screen("search_add")
    object Collections : Screen("collections")
    object Settings : Screen("settings")
    object WorkDetail : Screen("work_detail/{workId}") {
        fun createRoute(workId: Long) = "work_detail/$workId"
    }
    object WorkForm : Screen("work_form?workId={workId}&externalId={externalId}") {
        fun createRoute(workId: Long = 0, externalId: String? = null): String {
            return if (!externalId.isNullOrBlank()) {
                "work_form?workId=$workId&externalId=$externalId"
            } else {
                "work_form?workId=$workId"
            }
        }
    }
}
