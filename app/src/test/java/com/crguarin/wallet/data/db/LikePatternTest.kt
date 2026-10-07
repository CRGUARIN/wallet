package com.crguarin.wallet.data.db

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LikePatternTest {
    @Test
    fun `plain text is unchanged`() {
        assertThat("almuerzo familiar".escapeLikePattern()).isEqualTo("almuerzo familiar")
    }

    @Test
    fun `wildcards and the escape character are escaped`() {
        assertThat("50%".escapeLikePattern()).isEqualTo("50\\%")
        assertThat("a_b".escapeLikePattern()).isEqualTo("a\\_b")
        assertThat("c:\\tmp".escapeLikePattern()).isEqualTo("c:\\\\tmp")
    }
}
