import Mailserver.UserDb.UserDbEnv
import Mailserver.UserEmailer.UserEmailerEnv
import Mailserver.UserSubscription.UserSubscriptionEnv
import zio.{ExitCode, Has, Task, ULayer, URIO, ZIO, ZLayer}

object Mailserver extends zio.App {
  case class User(name: String, email: String)

  //------------------------------------------------------ UserEmailer
  object UserEmailer {
    // can add
    type UserEmailerEnv = Has[UserEmailer.Service]

    // service def
    trait Service {
      def notify(user: User, message: String): Task[Unit]
    }

    // service impl
    //val live: ZLayer[Any, Nothing, Has[Service]] = ZLayer.succeed {
    val live: ZLayer[Any, Nothing, UserEmailerEnv] = ZLayer.succeed {
      new Service {
        override def notify(user: User, message: String) = Task {
          println(s"[User emailer] Sending '$message' to ${user.email}")
        }
      }
    }

    // front API
    def notify(user: User, message: String): ZIO[UserEmailerEnv, Throwable, Unit] =
    //def notify(user: User, message: String): ZIO[Has[UserEmailer.Service], Throwable, Unit] =
      ZIO.accessM { hasService =>
        hasService.get.notify(user, message)
      }
  }

  //------------------------------------------------------ UserDb
  object UserDb {
    type UserDbEnv = Has[UserDb.Service]

    trait Service {
      def insert(user: User): Task[Unit]
    }

    val live = ZLayer.succeed(new Service {
      override def insert(user: User) = Task {
        println(s"[Db] insert into DB ${user.email}")
      }
    })
  }

  def insert(user: User): ZIO[UserDbEnv, Throwable, Unit] = ZIO.access(_.get.insert(user))

  //------------------------------------------------------ HORIZONTAL composition
  val userBackendLayer: ZLayer[Any, Nothing, UserDbEnv with UserEmailerEnv] = UserDb.live ++ UserEmailer.live

  //------------------------------------------------------ VERTICAL composition
  object UserSubscription {
    type UserSubscriptionEnv = Has[UserSubscription.Service]

    class Service(emailer: UserEmailer.Service, db: UserDb.Service) {
      def subscribe(user: User): Task[User] = {
        for {
          _ <- db.insert(user)
          _ <- emailer.notify(user, "Welcome")
        } yield user
      }
    }

    val live = ZLayer.fromServices[UserEmailer.Service, UserDb.Service, UserSubscription.Service] { (emaile, dbs) =>
      new Service(emaile, dbs)
    }

    def subscribe(user: User): ZIO[UserSubscriptionEnv, Throwable, User] = ZIO.accessM(_.get.subscribe(user))

  }
  //------------------------------------------------------ output
  val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscriptionEnv] = userBackendLayer >>> UserSubscription.live

  val user1 = User("Valentin", "valentins-email")
  val message = "Hallo"

//  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
//    UserEmailer.notify(user1, message)
//      //.provideLayer(UserEmailer.live)
//      .provideLayer(userBackendLayer)
//      .exitCode

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    UserSubscription.subscribe(user1)
      .provideLayer(userSubscriptionLayer)
      .exitCode
}



