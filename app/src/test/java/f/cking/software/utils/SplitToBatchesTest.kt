package f.cking.software.utils

import f.cking.software.splitToBatches
import junit.framework.TestCase.assertEquals
import org.junit.Test

class SplitToBatchesTest {

    @Test
    fun `test list size is less than batch`() {
        testSplitToBatches(
            listSize = 0,
            batchSize = 1,
            expected = listOf(emptyList())
        )
        testSplitToBatches(
            listSize = 1,
            batchSize = 2,
            expected = listOf(listOf(0))
        )
    }

    @Test
    fun `test list size is equal to batch size`() {
        testSplitToBatches(
            listSize = 1,
            batchSize = 1,
            expected = listOf(listOf(0))
        )
    }

    @Test
    fun `test batch size is grater than list`() {
        testSplitToBatches(
            listSize = 5,
            batchSize = 10,
            expected = listOf(listOf(0, 1, 2, 3, 4))
        )
    }

    @Test
    fun `test general scenarios`() {
        testSplitToBatches(
            listSize = 2,
            batchSize = 1,
            expected = listOf(listOf(0), listOf(1))
        )
        testSplitToBatches(
            listSize = 2,
            batchSize = 2,
            expected = listOf(listOf(0, 1))
        )
        testSplitToBatches(
            listSize = 3,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2))
        )
        testSplitToBatches(
            listSize = 4,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3))
        )
        testSplitToBatches(
            listSize = 5,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4))
        )
        testSplitToBatches(
            listSize = 6,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5))
        )
        testSplitToBatches(
            listSize = 7,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6))
        )
        testSplitToBatches(
            listSize = 8,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6, 7))
        )
    }

    private fun testSplitToBatches(listSize: Int, batchSize: Int, expected: List<List<Int>>) {
        val list: List<Int> = (0 until listSize).toList()
        val actual = list.splitToBatches(batchSize)
        assertEquals(expected, actual)
    }
}