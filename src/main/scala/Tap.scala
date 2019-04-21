import scalaz.zio.{IO, Ref, UIO, ZIO}

/**
  * A `Tap` adjusts the flow of tasks through
  * an external service in response to observed
  * failures in the service, always trying to
  * maximize flow while attempting to meet the
  * user-defined upper bound on failures.
  */
trait Tap[-E1, +E2] {

  /**
    * Sends the task through the tap. The
    * returned task may fail immediately with a
    * default error depending on the service
    * being guarded by the tap.
    */
  def apply[R, E >: E2 <: E1, A](effect: ZIO[R, E, A]): ZIO[R, E, A]
}

object Tap {
  type Percentage = Double

  /**
    * Creates a tap that aims for the specified
    * maximum error rate, using the specified
    * function to qualify errors (unqualified
    * errors are not treated as failures for
    * purposes of the tap), and the specified
    * default error used for rejecting tasks
    * submitted to the tap.
    */
  def make[E1, E2](
      errBound: Percentage,
      qualified: E1 => Boolean,
      rejected: => E2
  ): UIO[Tap[E1, E2]] =
    for {
      ref <- Ref.make(TapState(0, 0))
    } yield
      new Tap[E1, E2] {
        def apply[R, E >: E2 <: E1, A](effect: ZIO[R, E, A]): ZIO[R, E, A] = {

          def failure(e: E): IO[E, Nothing] =
            ref.update(_.update(qualified(e))) *> ZIO.fail(e)

          def success(a: A): UIO[A] =
            ref.update(_.update(false)) *> ZIO.succeed(a)

          for {
            state <- ref.get
            a <- if (state.errorRate <= errBound)
              effect.foldM(failure, success)
            else
              ref.update(_.update(false)) *> ZIO.fail(rejected)
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
