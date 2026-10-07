package com.crguarin.wallet.data.db

/** Escape character declared by the `LIKE ... ESCAPE` clauses of the DAOs. */
internal const val LIKE_ESCAPE_CHAR = '\\'

/**
 * Escapes the `LIKE` wildcards (`%`, `_`) and the escape character itself, so user text is
 * matched literally by a `LIKE '%' || :query || '%' ESCAPE '\'` clause.
 */
internal fun String.escapeLikePattern(): String =
    buildString(length) {
        for (ch in this@escapeLikePattern) {
            if (ch == LIKE_ESCAPE_CHAR || ch == '%' || ch == '_') append(LIKE_ESCAPE_CHAR)
            append(ch)
        }
    }
