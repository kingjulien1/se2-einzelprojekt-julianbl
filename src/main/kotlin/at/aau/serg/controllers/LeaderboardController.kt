package at.aau.serg.controllers

import at.aau.serg.models.GameResult
import at.aau.serg.services.GameResultService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/leaderboard")
class LeaderboardController(
    private val gameResultService: GameResultService
) {

    @GetMapping
    fun getLeaderboard(): List<GameResult> =
        gameResultService.getGameResults()
            // Sort by score descending: higher score is better / sort by score descending
            .sortedWith(
                compareByDescending<GameResult> { it.score }
                    // For equal scores, shorter play time is better / sort by time ascending
                    .thenBy { it.timeInSeconds }
            )
}