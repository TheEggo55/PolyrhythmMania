package polyrhythmmania.engine.input


/**
 * A score data structure for the results.
 */
data class Score(val scoreInt: Int, val scoreRaw: Float, val nInputs: Int,
                 val skillStar: Boolean, val noMiss: Boolean,
                 val title: String, val line1: String, val line2: String = "",
                 val ranking: Ranking = Ranking.getRanking(scoreInt)) {
    
    val butStillJustOk: Boolean = ranking == Ranking.OK && scoreInt >= 75
}
