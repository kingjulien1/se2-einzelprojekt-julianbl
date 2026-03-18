package at.aau.serg.controllers

import at.aau.serg.models.GameResult
import at.aau.serg.services.GameResultService
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.mockito.Mockito.`when` as whenever // when is a reserved keyword in Kotlin

class LeaderboardControllerTests {

    private lateinit var mockedService: GameResultService
    private lateinit var controller: LeaderboardController

    @BeforeEach
    fun setup() {
        mockedService = mock<GameResultService>()
        controller = LeaderboardController(mockedService)
    }

    @Test
    fun test_getLeaderboard_correctScoreSorting() {
        val first = GameResult(1, "first", 20, 20.0)
        val second = GameResult(2, "second", 15, 10.0)
        val third = GameResult(3, "third", 10, 15.0)

        // Return the results in unsorted order from the mocked service
        whenever(mockedService.getGameResults()).thenReturn(listOf(second, first, third))

        // Call the controller without rank to get the full leaderboard
        val res: List<GameResult> = controller.getLeaderboard(null)

        verify(mockedService).getGameResults()
        assertEquals(3, res.size)
        assertEquals(first, res[0])
        assertEquals(second, res[1])
        assertEquals(third, res[2])
    }

    @Test
    fun test_getLeaderboard_sameScore_CorrectTimeSorting() {
        val slowest = GameResult(1, "first", 20, 20.0)
        val fastest = GameResult(2, "second", 20, 10.0)
        val middle = GameResult(3, "third", 20, 15.0)

        // Return players with identical score but different times
        whenever(mockedService.getGameResults()).thenReturn(listOf(middle, slowest, fastest))

        // Without rank, the complete sorted leaderboard should be returned
        val res: List<GameResult> = controller.getLeaderboard(null)

        verify(mockedService).getGameResults()
        assertEquals(3, res.size)
        assertEquals(fastest, res[0])
        assertEquals(middle, res[1])
        assertEquals(slowest, res[2])
    }

    @Test
    fun test_getLeaderboard_rankInMiddle_returnsPlayerWithThreeAboveAndBelow() {
        val p1 = GameResult(1, "p1", 100, 10.0)
        val p2 = GameResult(2, "p2", 90, 11.0)
        val p3 = GameResult(3, "p3", 80, 12.0)
        val p4 = GameResult(4, "p4", 70, 13.0)
        val p5 = GameResult(5, "p5", 60, 14.0)
        val p6 = GameResult(6, "p6", 50, 15.0)
        val p7 = GameResult(7, "p7", 40, 16.0)
        val p8 = GameResult(8, "p8", 30, 17.0)
        val p9 = GameResult(9, "p9", 20, 18.0)

        // Return the players in mixed order so the controller has to sort them first
        whenever(mockedService.getGameResults()).thenReturn(listOf(p9, p7, p5, p3, p1, p8, p6, p4, p2))

        // Request rank 5 and expect the selected player plus up to 3 above and below
        val res: List<GameResult> = controller.getLeaderboard(5)

        verify(mockedService).getGameResults()
        assertEquals(7, res.size)
        assertEquals(p2, res[0])
        assertEquals(p3, res[1])
        assertEquals(p4, res[2])
        assertEquals(p5, res[3])
        assertEquals(p6, res[4])
        assertEquals(p7, res[5])
        assertEquals(p8, res[6])
    }

    @Test
    fun test_getLeaderboard_rankAtStart_returnsOnlyAvailableFollowingPlayers() {
        val p1 = GameResult(1, "p1", 100, 10.0)
        val p2 = GameResult(2, "p2", 90, 11.0)
        val p3 = GameResult(3, "p3", 80, 12.0)
        val p4 = GameResult(4, "p4", 70, 13.0)
        val p5 = GameResult(5, "p5", 60, 14.0)

        // Return the players in mixed order to verify sorting before window selection
        whenever(mockedService.getGameResults()).thenReturn(listOf(p5, p3, p1, p4, p2))

        // Rank 1 has no players above it, so only the following ranks should be returned
        val res: List<GameResult> = controller.getLeaderboard(1)

        verify(mockedService).getGameResults()
        assertEquals(4, res.size)
        assertEquals(p1, res[0])
        assertEquals(p2, res[1])
        assertEquals(p3, res[2])
        assertEquals(p4, res[3])
    }

    @Test
    fun test_getLeaderboard_rankAtEnd_returnsOnlyAvailablePreviousPlayers() {
        val p1 = GameResult(1, "p1", 100, 10.0)
        val p2 = GameResult(2, "p2", 90, 11.0)
        val p3 = GameResult(3, "p3", 80, 12.0)
        val p4 = GameResult(4, "p4", 70, 13.0)
        val p5 = GameResult(5, "p5", 60, 14.0)

        // Return the players in mixed order to verify sorting before slicing the window
        whenever(mockedService.getGameResults()).thenReturn(listOf(p4, p2, p5, p1, p3))

        // The last rank has no players below it, so only the previous ranks are included
        val res: List<GameResult> = controller.getLeaderboard(5)

        verify(mockedService).getGameResults()
        assertEquals(4, res.size)
        assertEquals(p2, res[0])
        assertEquals(p3, res[1])
        assertEquals(p4, res[2])
        assertEquals(p5, res[3])
    }

    @Test
    fun test_getLeaderboard_rankZero_throwsBadRequest() {
        val only = GameResult(1, "only", 10, 10.0)

        whenever(mockedService.getGameResults()).thenReturn(listOf(only))

        // Rank 0 is invalid and should cause HTTP 400
        val ex = assertFailsWith<ResponseStatusException> {
            controller.getLeaderboard(0)
        }

        verify(mockedService).getGameResults()
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun test_getLeaderboard_rankTooLarge_throwsBadRequest() {
        val first = GameResult(1, "first", 20, 20.0)
        val second = GameResult(2, "second", 10, 10.0)

        whenever(mockedService.getGameResults()).thenReturn(listOf(first, second))

        // A rank larger than the leaderboard size is invalid
        val ex = assertFailsWith<ResponseStatusException> {
            controller.getLeaderboard(3)
        }

        verify(mockedService).getGameResults()
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun test_getLeaderboard_negativeRank_throwsBadRequest() {
        val only = GameResult(1, "only", 10, 10.0)

        whenever(mockedService.getGameResults()).thenReturn(listOf(only))

        // Negative ranks are invalid and should cause HTTP 400
        val ex = assertFailsWith<ResponseStatusException> {
            controller.getLeaderboard(-1)
        }

        verify(mockedService).getGameResults()
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }
}