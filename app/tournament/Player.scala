package lila
package tournament

import com.mongodb.casbah.query.Imports._

import user.User

case class Player(
    id: String,
    username: String,
    elo: Int,
    withdraw: Boolean = false,
    nbWin: Int = 0,
    nbLoss: Int = 0,
    winStreak: Int = 0,
    score: Int = 0) {

  def active = !withdraw

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)

  def doWithdraw = copy(withdraw = true)
}

object Player {

  def apply(user: User): Player = new Player(
    id = user.id,
    username = user.username,
    elo = user.elo)

  def refresh(tour: Tournament): Players = tour.players.map { player ⇒
    tour.pairings
      .filter(p ⇒ p.finished && (p contains player.id))
      .foldLeft(Builder(player))(_ + _.winner)
      .toPlayer
  } sortBy { p ⇒ 
    p.withdraw.fold(Int.MaxValue, 0) - p.score 
  }

  private case class Builder(
      player: Player,
      nbWin: Int = 0,
      nbLoss: Int = 0,
      score: Int = 0,
      winSeq: Int = 0,
      bestWinSeq: Int = 0,
      prevWin: Boolean = false) {

    def +(winner: Option[String]) = {
      val (win, loss) = winner.fold(
        w ⇒ if (w == player.id) true -> false else false -> true,
        false -> false)
      val newWinSeq = if (win) prevWin.fold(winSeq + 1, 1) else 0
      val points = win.fold(1 + newWinSeq, loss.fold(0, 1))
      copy(
        nbWin = nbWin + win.fold(1, 0),
        nbLoss = nbLoss + loss.fold(1, 0),
        score = score + points,
        winSeq = newWinSeq,
        bestWinSeq = math.max(bestWinSeq, newWinSeq),
        prevWin = win)
    }

    def toPlayer = player.copy(
      nbWin = nbWin,
      nbLoss = nbLoss,
      winStreak = bestWinSeq,
      score = score)
  }
}
