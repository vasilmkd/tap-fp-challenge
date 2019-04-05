import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.implicits._

trait Tap {
  def apply[A](effect: IO[A]): IO[A]
}

object Tap {
  type Percentage = Double

  def make(
      errBound: Percentage,
      qualified: Throwable => Boolean,
      rejected: => Throwable
  ): IO[Tap] =
    for {
      ref <- Ref.of[IO, TapState](TapState(0, 0))
    } yield
      new Tap {
        def apply[A](effect: IO[A]): IO[A] = {

          def bind[A](a: A): IO[A] =
            ref.update(_.update(false)) *> IO.pure(a)

          def recover[A](e: Throwable): IO[A] =
            ref.update(_.update(qualified(e))) *> IO.raiseError(e)

          for {
            state <- ref.get
            a <- if (state.errorRate <= errBound)
              effect.redeemWith(recover, bind)
            else
              ref.update(_.update(false)) *> IO.raiseError(rejected)
          } yield a
        }
      }

  private case class TapState(errCount: Int, count: Int) {
    def errorRate: Double =
      if (count <= 0) 0.0
      else errCount.toDouble / count.toDouble

    def update(qualified: Boolean): TapState =
      if (qualified) TapState(errCount + 1, count + 1)
      else TapState(errCount, count + 1)
  }
}
