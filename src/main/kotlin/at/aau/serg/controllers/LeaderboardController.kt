package at.aau.serg.controllers

import at.aau.serg.models.GameResult
import at.aau.serg.services.GameResultService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/leaderboard")
class LeaderboardController(
    private val gameResultService: GameResultService
) {

    @GetMapping
    fun getLeaderboard(@RequestParam(required = false) rank: Int? = null): List<GameResult> {
        // Get all stored game results from the service
        val leaderboard = gameResultService.getGameResults()
            // Sort by score descending: higher score is better
            .sortedWith(
                compareByDescending<GameResult> { it.score }
                    // For equal scores, shorter play time is better
                    .thenBy { it.timeInSeconds }
            )

        // If no rank is given, return the full leaderboard
        if (rank == null) {
            return leaderboard
        }

        // Reject invalid rank values with HTTP 400
        if (rank < 1 || rank > leaderboard.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rank")
        }

        return leaderboard;
    }
}