import zio.{ExitCode, URIO, ZIO}
import zio.console.{getStrLn, putStrLn}

object Intro extends zio.App{
  // Zlayers
  // ZIO[-R +E +A]
  // R => Either[E, A]

  val ml = ZIO.succeed(42)
  val f = ZIO.fail("failier")

  val greeting = for {
    _ <- putStrLn("Hello, Welcome")
    name <- getStrLn
    _ <- putStrLn(s"Hello $name")
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = greeting.exitCode
}
