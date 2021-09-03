

import actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport


class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindTypedActor(UserSessionManagerActor(), "userSessionManagerActor")
  }
}

// @Singleton
// class UserActorFactoryProvider @Inject()(
//     stocksActor: ActorRef[StocksActor.GetStocks],
//     mat: Materializer,
//     ec: ExecutionContext,
// ) extends Provider[UserActor.Factory] {
//   def get() = UserActor(_, stocksActor)(mat, ec)
// }
